package dev.turingcomplete.bitbucketcodecoverage.jacoco

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

abstract class PublishJacocoCodeCoverageToBitbucketExtension {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  abstract val jacocoXmlCoverageReports: ConfigurableFileCollection

  abstract val skipOnMissingJacocoXmlCoverageReports: Property<Boolean>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    @Suppress("LeakingThis")
    skipOnMissingJacocoXmlCoverageReports.convention(false)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}