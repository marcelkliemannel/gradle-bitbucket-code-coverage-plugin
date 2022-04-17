package dev.turingcomplete.bitbucketcodecoverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.util.*


abstract class PublishCodeCoverageToBitbucketTask : DefaultTask() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  /**
   * The host address to the Bitbucket API (e.g., `https://bitbucket.inc.com`).
   */
  @get:Input
  abstract val bitbucketApiHost: Property<String>

  @get:[Optional Input]
  abstract val bitbucketApiUser: Property<String>

  @get:[Optional Input]
  abstract val bitbucketApiPassword: Property<String>

  @get:[Optional Input]
  abstract val bitbucketApiToken: Property<String>

  /**
   * The request timeout for the Bitbucket API. The default value is 30 seconds.
   */
  @get:[Optional Input]
  abstract val bitbucketApiTimeout: Property<Duration>

  /**
   * The project key to which the [bitbucketRepositorySlug] belongs.
   *
   * If this property is set, [bitbucketRepositorySlug] must also be set.
   */
  @get:[Optional Input]
  abstract val bitbucketProjectKey: Property<String>

  /**
   * The repository to which the [bitbucketCommitId] belongs.
   *
   * If this property is set, [bitbucketProjectKey] must also be set.
   */
  @get:[Optional Input]
  abstract val bitbucketRepositorySlug: Property<String>

  /**
   * The Git commit ID to which the published code coverage should be associated.
   *
   * If [bitbucketProjectKey] and [bitbucketRepositorySlug] is not set,
   * the Bitbucket API will add the code coverage to all repositories which
   * have a commit with this ID.
   */
  @get:Input
  abstract val bitbucketCommitId: Property<String>

  /**
   *
   */
  @get:InputFiles
  abstract val sourceFilesSearchDirs: ConfigurableFileCollection

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    @Suppress("LeakingThis")
    bitbucketApiTimeout.convention(Duration.ofSeconds(30))
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @TaskAction
  fun publish() {
    val classCodeCoverages = collectClassCodeCoverages().ifEmpty { return }

    findSourceFileMappings(classCodeCoverages)

    sendToBitbucketApi(classCodeCoverages)
  }

  abstract fun collectClassCodeCoverages(): List<ClassCodeCoverage>

  /**
   * Creates a [HttpClient] that gets used to communicate with the Bitbucket API.
   */
  open fun createBitbucketApiHttpClient(): HttpClient {
    return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
  }

  /**
   * Creates a [HttpRequest] which publishes the given Bitbucket code coverage
   * JSON to the Bitbucket API.
   *
   * See comment on [PublishCodeCoverageToBitbucketTask] for the authentication
   * handling.
   */
  open fun createPublishBitbucketCodeCoverageRequest(bitbucketCodeCoverageJson: String): HttpRequest {
    val createPublishBitbucketCodeCoverageRequestUri = createPublishBitbucketCodeCoverageRequestUri()
    val request = HttpRequest.newBuilder()
            .uri(createPublishBitbucketCodeCoverageRequestUri)
            .timeout(bitbucketApiTimeout.get())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bitbucketCodeCoverageJson))

    if (bitbucketApiUser.isPresent && bitbucketApiPassword.isPresent) {
      request.header("Authorization", "Basic ${Base64.getEncoder().encodeToString("${bitbucketApiUser.get()}:${bitbucketApiPassword.get()}".toByteArray())}")
    }
    else if (bitbucketApiToken.isPresent) {
      request.header("Authorization", "Bearer ${bitbucketApiToken.isPresent}")
    }

    return request.build()
  }

  /**
   * Creates the full [URI] to the Bitbucket API endpoint to publish the code
   * coverage data for a specific commit.
   *
   * @return the full [URI] to the Bitbucket code coverage API endpoint.
   */
  open fun createPublishBitbucketCodeCoverageRequestUri(): URI {
    return URI.create(StringBuilder().apply {
      // Append trailing slash to host if it is not present yet
      append(bitbucketApiHost.get().replace(Regex("/?$"), "/"))

      append("bitbucket/rest/code-coverage/1.0/")

      if (bitbucketProjectKey.isPresent && bitbucketRepositorySlug.isPresent) {
        append("projects/${bitbucketProjectKey.get()}/repos/${bitbucketRepositorySlug.get()}/")
      }

      append("commits/${bitbucketCommitId.get()}")
    }.toString())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun sendToBitbucketApi(classCodeCoverages: List<ClassCodeCoverage>) {
    logger.debug("Sending code coverage to Bitbucket...")

    val jsonRepresentation = createBitbucketApiJsonRepresentation(classCodeCoverages)
    val request = createPublishBitbucketCodeCoverageRequest(jsonRepresentation)
    logger.debug("Accessing the Bitbucket API via URI ${request.uri()} and body:\n${jsonRepresentation}")

    val response: HttpResponse<String> = createBitbucketApiHttpClient().send(request, BodyHandlers.ofString())
    if (response.statusCode() !in (200.. 299)) {
      val body = response.body()
      throw GradleException("The Bitbucket API responded with the unexpected status code ${response.statusCode()}${if (body.isNotBlank()) " and body:\n$body" else "."}")
    }
  }

  private fun createBitbucketApiJsonRepresentation(classCodeCoverages: List<ClassCodeCoverage>): String {
    val rootProjectDir = project.rootProject.rootDir.toPath()

    return """
      {
        "files": [
          ${classCodeCoverages.filter { it.sourceFile != null }.joinToString(",\n") {
      """
          {
            "path": "${rootProjectDir.relativize(it.sourceFile!!)}",
            "coverage": "${it.toBitbucketCodeCoverage()}"
          }
          """.trim()
    }}
        ]
      }
    """.trimIndent()
  }

  /**
   * Searches for the [ClassCodeCoverage.sourceFile] mappings by traversing
   * the [sourceFilesSearchDirs].
   */
  private fun findSourceFileMappings(classCodeCoverages: List<ClassCodeCoverage>) {
    classCodeCoverages.ifEmpty { return }

    val sourceFilesToFind = createPathMatchersForSourceFiles(classCodeCoverages).toMutableMap()
    sourceFilesSearchDirs.files.takeWhile { sourceFilesToFind.isNotEmpty() }.filter { it.exists() }.forEach { sourceFilesSearchDir ->
      logger.debug("Searching for source file mappings in directory: $sourceFilesSearchDir")
      Files.walkFileTree(sourceFilesSearchDir.toPath(), createSourcesFilesSearchDirsFileVisitor(sourceFilesToFind))
    }

    if (logger.isWarnEnabled && sourceFilesToFind.isNotEmpty()) {
      val warnMessage = StringBuilder("Can't find a source file mapping for the following entries:\n")
      sourceFilesToFind.values.forEach { warnMessage.append(" - ${it.getRelativeSourceFile()}\n") }
      warnMessage.append("Searched in the following directories:\n")
      sourceFilesSearchDirs.forEach { warnMessage.append("- $it\n") }
      logger.warn(warnMessage.toString())
    }
  }

  /**
   * Creates a [PathMatcher] for each given [ClassCodeCoverage] to find its
   * [ClassCodeCoverage.sourceFile], which relative file path is given by its
   * package name and file name.
   */
  private fun createPathMatchersForSourceFiles(classCodeCoverages: List<ClassCodeCoverage>): Map<PathMatcher, ClassCodeCoverage> {
    val fileSystem = FileSystems.getDefault()

    return classCodeCoverages.associateBy {
      fileSystem.getPathMatcher("glob:**/" + it.getRelativeSourceFile())
    }
  }

  private fun createSourcesFilesSearchDirsFileVisitor(sourceFilesToFind: MutableMap<PathMatcher, ClassCodeCoverage>): FileVisitor<Path> {
    return object : SimpleFileVisitor<Path>() {

      override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
        sourceFilesToFind.asSequence().find { it.key.matches(file) }?.let {
          val classCodeCoverage = it.value
          logger.debug("Mapping source file ${classCodeCoverage.sourceFileName} in package ${classCodeCoverage.packageName} to file: $file")

          classCodeCoverage.sourceFile = file

          sourceFilesToFind.remove(it.key)
        }

        return if (sourceFilesToFind.isEmpty()) FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}