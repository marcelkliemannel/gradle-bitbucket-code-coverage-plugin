package dev.turingcomplete.bitbucketcodecoverage.jacoco

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.testing.jacoco.tasks.JacocoReport
import javax.inject.Inject

/**
 * Extension to configure the built-in [PublishJacocoCodeCoverageToBitbucketTask].
 */
open class PublishJacocoCodeCoverageToBitbucketExtension @Inject constructor(objects: ObjectFactory) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  /**
   * The XML JaCoCo report files which will be converted to Bitbucket coverage.
   *
   * If non are set explicitly, it will be set to the results of the
   * [JacocoReport] task.
   */
  val jacocoXmlCoverageReports: ConfigurableFileCollection

  /**
   * Ignores missing [jacocoXmlCoverageReports].
   *
   * By default, it's set to false.
   */
  val skipOnMissingJacocoXmlCoverageReports: Property<Boolean>

  /**
   * Directories to search for source files. Will be populated with all source
   * directories from all [SourceSet]s by default.
   *
   * Jacoco only references the package name and the file name of the source
   * files in its code coverage report. But Bitbucket needs the path to the
   * source files relative to the project root directories.
   */
  val sourceFilesSearchDirs: ConfigurableFileCollection

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    jacocoXmlCoverageReports = objects.fileCollection()
    skipOnMissingJacocoXmlCoverageReports = objects.property(Boolean::class.java)
    sourceFilesSearchDirs = objects.fileCollection()

    // Set default values
    skipOnMissingJacocoXmlCoverageReports.convention(false)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}