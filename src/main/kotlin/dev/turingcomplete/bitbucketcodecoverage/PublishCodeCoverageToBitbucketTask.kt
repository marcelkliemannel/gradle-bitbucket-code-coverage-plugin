package dev.turingcomplete.bitbucketcodecoverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.*

/**
 * A task that sends code coverage, which are represented by [FileCodeCoverage],
 * to Bitbucket.
 */
abstract class PublishCodeCoverageToBitbucketTask : DefaultTask() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val BITBUCKET_API_HOST_VALIDATION_PATTERN = Regex("^https?://.*$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  /**
   * The host address to Bitbucket (e.g., `https://bitbucket.inc.com`).
   *
   * Must start with `https://` or `http://`.
   */
  @get:Input
  val bitbucketHost: Property<String>

  /**
   * A Bitbucket user for the authentication.
   *
   * Must be set in conjunction with [bitbucketPassword].
   */
  @get:[Optional Input]
  val bitbucketUser: Property<String>

  /**
   * The password of the [bitbucketUser].
   *
   * Must be set in conjunction with [bitbucketUser].
   */
  @get:[Optional Input]
  val bitbucketPassword: Property<String>

  /**
   * A token to be used for authentication as an alternative to the
   * user/password authentication.
   *
   * The value will be ignored if [bitbucketUser] is set.
   */
  @get:[Optional Input]
  val bitbucketToken: Property<String>

  /**
   * The timeout of any request to Bitbucket. The default value is 30 seconds.
   */
  @get:[Optional Input]
  val bitbucketTimeout: Property<Duration>

  /**
   * The Git commit ID to which the published code coverage should be associated.
   *
   * If [bitbucketProjectKey] and [bitbucketRepositorySlug] is not set,
   * Bitbucket will add the code coverage to all repositories which have a
   * commit with this ID.
   */
  @get:Input
  val bitbucketCommitId: Property<String>

  /**
   * A Bitbucket project key.
   *
   * Must be set in conjunction with [bitbucketRepositorySlug].
   */
  @get:[Optional Input]
  val bitbucketProjectKey: Property<String>

  /**
   * A repository slug.
   *
   * Must be set in conjunction with [bitbucketProjectKey].
   */
  @get:[Optional Input]
  val bitbucketRepositorySlug: Property<String>

  @get:Input
  val fileCodeCoverages: ListProperty<FileCodeCoverage>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val objects = project.objects
    bitbucketHost = objects.property(String::class.java)
    bitbucketUser = objects.property(String::class.java)
    bitbucketPassword = objects.property(String::class.java)
    bitbucketToken = objects.property(String::class.java)
    bitbucketTimeout = objects.property(Duration::class.java)
    bitbucketProjectKey = objects.property(String::class.java)
    bitbucketRepositorySlug = objects.property(String::class.java)
    bitbucketCommitId = objects.property(String::class.java)
    fileCodeCoverages = objects.listProperty(FileCodeCoverage::class.java)

    // Set default values
    bitbucketTimeout.convention(Duration.ofSeconds(30))
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @TaskAction
  fun publish() {
    validateTaskProperties()
    publishCodeCoverageToBitbucketApi(fileCodeCoverages.get())
  }

  /**
   * Creates an [HttpClient] that gets used to communicate with Bitbucket.
   *
   * @return an [HttpClient] for the communication with Bitbucket.
   */
  open fun createBitbucketHttpClient(): HttpClient {
    return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
  }

  /**
   * Creates an [HttpRequest] which gets used to publish the given code coverage
   * as a JSON representation to Bitbucket.
   *
   * @return an [HttpRequest] to publish the code coverage to Bitbucket.
   */
  open fun createPublishBitbucketCodeCoverageRequest(bitbucketCodeCoverageJson: String): HttpRequest {
    val request = HttpRequest.newBuilder()
            .uri(createPublishBitbucketCodeCoverageRequestUri())
            .timeout(bitbucketTimeout.get())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bitbucketCodeCoverageJson))

    if (bitbucketUser.isPresent && bitbucketPassword.isPresent) {
      request.header("Authorization", "Basic ${Base64.getEncoder().encodeToString("${bitbucketUser.get()}:${bitbucketPassword.get()}".toByteArray())}")
    }
    else if (bitbucketToken.isPresent) {
      request.header("Authorization", "Bearer ${bitbucketToken.get()}")
    }

    return request.build()
  }

  /**
   * Creates the full [URI] to the Bitbucket endpoint which consumes the code
   * coverage for a specific commit ID.
   *
   * The default URIs are:
   * - `$host/rest/code-coverage/1.0/commits/$commitID` or
   * - `$host/rest/code-coverage/1.0/projects/$projectKey/repos/$repositorySlug/commits/$commitID`.
   *
   * @return the full [URI] to the Bitbucket code coverage endpoint.
   */
  open fun createPublishBitbucketCodeCoverageRequestUri(): URI {
    return URI.create(StringBuilder().apply {
      // Append trailing slash to host if it is not present yet
      append(bitbucketHost.get().replace(Regex("/?$"), "/"))

      append("rest/code-coverage/1.0/")

      if (bitbucketProjectKey.isPresent && bitbucketRepositorySlug.isPresent) {
        append("projects/${bitbucketProjectKey.get()}/repos/${bitbucketRepositorySlug.get()}/")
      }

      append("commits/${bitbucketCommitId.get()}")
    }.toString())
  }

  /**
   * Configures this task from the given [PublishCodeCoverageToBitbucketExtension].
   */
  open fun configureFromExtension(extension: PublishCodeCoverageToBitbucketExtension) {
    bitbucketHost.set(extension.bitbucketHost)
    bitbucketUser.set(extension.bitbucketUser)
    bitbucketPassword.set(extension.bitbucketPassword)
    bitbucketToken.set(extension.bitbucketToken)
    bitbucketTimeout.set(extension.bitbucketTimeout)
    bitbucketCommitId.set(extension.bitbucketCommitId)
    bitbucketProjectKey.set(extension.bitbucketProjectKey)
    bitbucketRepositorySlug.set(extension.bitbucketRepositorySlug)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun validateTaskProperties() {
    if (!BITBUCKET_API_HOST_VALIDATION_PATTERN.matches(bitbucketHost.get())) {
      throw GradleException("Bitbucket host must start with 'http://' or 'https://'.")
    }
  }

  private fun publishCodeCoverageToBitbucketApi(fileCodeCoverages: List<FileCodeCoverage>) {
    if (fileCodeCoverages.isEmpty()) {
      logger.warn("There are no code coverages available which can be published to Bitbucket.")
      return
    }

    // Send request
    val response = try {
      logger.info("Sending code coverage for ${fileCodeCoverages.size} file(s) to Bitbucket...")

      val jsonRepresentation = createBitbucketFileCoveragesJson(fileCodeCoverages)
      logger.debug("The following Bitbucket code coverage will be send:\n${jsonRepresentation}")

      val request = createPublishBitbucketCodeCoverageRequest(jsonRepresentation)
      logger.debug("Using Bitbucket URI: ${request.uri()}")

      createBitbucketHttpClient().send(request, BodyHandlers.ofString())
    }
    catch (e: Exception) {
      throw GradleException("Failed to send code coverage to Bitbucket.", e)
    }

    // Check response
    if (project.logger.isDebugEnabled) {
      val headersAsText = response.headers().map().map { "- ${it.key}: ${it.value.joinToString()}" }.joinToString("\n")
      logger.debug("Bitbucket responded with status code ${response.statusCode()}, headers:\n$headersAsText\n\nand body:\n${response.body()}")
    }
    
    if (response.statusCode() !in (200..299)) {
      throw GradleException("Failed to send code coverage to Bitbucket. Got unexpected status code ${response.statusCode()} and body:\n${response.body()}")
    }

    logger.info("Code coverage was sent to Bitbucket.")
  }

  /**
   * Creates the JSON content of the given `fileCodeCoverages` which gets send
   * to Bitbucket.
   *
   * We create the JSON object here manually to avoid an additional dependency
   * to the plugin.
   */
  private fun createBitbucketFileCoveragesJson(fileCodeCoverages: List<FileCodeCoverage>): String {
    assert(fileCodeCoverages.all { project.rootDir.resolve(it.sourceFile).exists() })

    return """
{
  "files": [
    |${
      fileCodeCoverages.joinToString(",\n") {
        """
    {
      "path": "${it.sourceFile}",
      "coverage": "${it.toBitbucketCodeCoverage()}"
    }
    """.trimMargin()
      }
    }
  ]
}
""".trimMargin()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}