@file:Suppress("LeakingThis")

package dev.turingcomplete.bitbucketcodecoverage.jacoco

import dev.turingcomplete.bitbucketcodecoverage.FileCodeCoverage
import dev.turingcomplete.bitbucketcodecoverage.PublishCodeCoverageToBitbucketExtension
import dev.turingcomplete.bitbucketcodecoverage.PublishCodeCoverageToBitbucketTask
import dev.turingcomplete.bitbucketcodecoverage.SourceFilesResolver
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import java.io.File
import java.util.*

/**
 * A [PublishCodeCoverageToBitbucketTask] which converts the generated XML
 * reports from the [org.gradle.testing.jacoco.tasks.JacocoReport] task and
 * publish them to Bitbucket.
 */
open class PublishJacocoCodeCoverageToBitbucketTask : PublishCodeCoverageToBitbucketTask() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val TASK_NAME: String = "publishJacocoCodeCoverageToBitbucket"

    private val MISSING_REPORT_ERROR_MESSAGE: String = """There are no JaCoCo XML reports available which can be converted.
  
  Reason: The JaCoCo plugin was not applied to the project, or the generation of XML reports was not activated (which is deactivated by default).
  
  Possible solutions:
    - Apply the JaCoCo plugin:
        plugins {
          jacoco
        }
  
    - Activate the XML report generation by setting the following property to the 'jacocoTestReport' task:
        tasks.jacocoTestReport {
          reports.xml.required.set(true)
        }
    
    - Suppress this error by ignoring missing JaCoCo report files:
        bitbucketCodeCoverage {
          jacoco {
            skipOnMissingJacocoXmlCoverageReports.set(true)
          }
        }
    """.trimIndent()
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  /**
   * The XML JaCoCo report files which will be converted to Bitbucket coverage.
   *
   * If non are set explicitly, it will be set to the results of the
   * [org.gradle.testing.jacoco.tasks.JacocoReport] task.
   */
  @get:InputFiles
  val jacocoXmlCoverageReports: ConfigurableFileCollection

  /**
   * Ignores missing [jacocoXmlCoverageReports].
   *
   * By default, it's set to false.
   */
  @get:[Input Optional]
  val skipOnMissingJacocoXmlCoverageReports: Property<Boolean>

  /**
   * Directories to search for source files. Will be populated with all source
   * directories from all [SourceSet]s by default.
   *
   * JaCoCo only references the package name and the file name of the source
   * files in its code coverage report. But Bitbucket needs the path to the
   * source files relative to the project root directories.
   */
  @get:[Optional InputFiles]
  val sourceFilesSearchDirs: ConfigurableFileCollection

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val objects = project.objects
    jacocoXmlCoverageReports = objects.fileCollection()
    skipOnMissingJacocoXmlCoverageReports = objects.property(Boolean::class.java)
    sourceFilesSearchDirs = objects.fileCollection()

    // Default values
    skipOnMissingJacocoXmlCoverageReports.convention(false)
    fileCodeCoverages.convention(project.provider { collectFileCodeCoverages() })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configureFromExtension(extension: PublishCodeCoverageToBitbucketExtension) {
    super.configureFromExtension(extension)

    skipOnMissingJacocoXmlCoverageReports.set(extension.jacoco.skipOnMissingJacocoXmlCoverageReports)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun collectFileCodeCoverages(): List<FileCodeCoverage> {
    val convertibleReports = jacocoXmlCoverageReports.filter { JacocoXmlReportConverter.isReportConvertible(it) }.files.ifEmpty {
      if (skipOnMissingJacocoXmlCoverageReports.get()) {
        return emptyList()
      }

      throw GradleException(MISSING_REPORT_ERROR_MESSAGE)
    }

    // Convert JaCoCo XML reports to `FileCodeCoverage.Builder` and their
    // simple source file paths (just package name + class name). The builder
    // needs to resolve the path of the source file to on which is relative to
    // the project root directory.
    val fileCodeCoverageBuilderToUnresolvedSourceFile = convertJacocoReports(convertibleReports)
    // Search a project root directory relative path for all simple source files.
    val resolvedSourceFiles = SourceFilesResolver.resolveSourceFilesRelativeToProjectRootDir(project, sourceFilesSearchDirs, fileCodeCoverageBuilderToUnresolvedSourceFile.values)

    return buildFileCodeCoverages(fileCodeCoverageBuilderToUnresolvedSourceFile, resolvedSourceFiles)
  }

  private fun convertJacocoReports(jacocoReports: Set<File>): Map<FileCodeCoverage.Builder, File> {
    val codeCoverageBuilderToUnresolvedSourceFile = mutableMapOf<FileCodeCoverage.Builder, File>()

    jacocoReports.forEach { jacocoReportFile ->
      JacocoXmlReportConverter.convertReport(jacocoReportFile).forEach {
        codeCoverageBuilderToUnresolvedSourceFile[it.key] = it.value
      }
    }

    return codeCoverageBuilderToUnresolvedSourceFile
  }

  private fun buildFileCodeCoverages(fileCodeCoverageBuilderToUnresolvedSourceFile: Map<FileCodeCoverage.Builder, File>,
                                     resolvedSourceFiles: Map<File, File>): List<FileCodeCoverage> {

    val fileCodeCoverages = mutableListOf<FileCodeCoverage>()
    val missingSourceFiles = mutableListOf<File>()

    fileCodeCoverageBuilderToUnresolvedSourceFile.forEach {
      if (resolvedSourceFiles.containsKey(it.value)) {
        fileCodeCoverages.add(it.key.build(resolvedSourceFiles[it.value]!!))
      }
      else {
        missingSourceFiles.add(it.value)
      }
    }

    warnAboutMissingSourceFiles(missingSourceFiles)

    return fileCodeCoverages
  }

  private fun warnAboutMissingSourceFiles(missingSourceFiles: List<File>) {
    if (!project.logger.isWarnEnabled || missingSourceFiles.isEmpty()) {
      return
    }

    val warnMessage = StringJoiner("\n")

    warnMessage.add("Can't find the following source files:")
    missingSourceFiles.forEach { warnMessage.add(" - $it") }

    if (sourceFilesSearchDirs.isEmpty) {
      warnMessage.add("There are no search directories configured.")
    }
    else {
      warnMessage.add("Searched in the following directories:")
      sourceFilesSearchDirs.forEach { warnMessage.add("- $it") }
    }

    project.logger.warn(warnMessage.toString())
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}