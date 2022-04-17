package dev.turingcomplete.bitbucketcodecoverage.jacoco

import dev.turingcomplete.bitbucketcodecoverage.ClassCodeCoverage
import dev.turingcomplete.bitbucketcodecoverage.PublishCodeCoverageToBitbucketTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional

abstract class PublishJacocoCodeCoverageToBitbucketTask : PublishCodeCoverageToBitbucketTask() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val TASK_NAME: String = "publishJacocoCodeCoverageToBitbucket"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  @get:InputFiles
  abstract val jacocoXmlCoverageReports: ConfigurableFileCollection

  @get:[Input Optional]
  abstract val skipOnMissingJacocoXmlCoverageReports: Property<Boolean>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    @Suppress("LeakingThis")
    skipOnMissingJacocoXmlCoverageReports.convention(false)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun collectClassCodeCoverages(): List<ClassCodeCoverage> {
    val convertibleJacocoReports = jacocoXmlCoverageReports.filter { JacocoXmlReportConverter.isReportConvertible(it) }
    if (convertibleJacocoReports.isEmpty && !skipOnMissingJacocoXmlCoverageReports.get()) {
      throw GradleException("""There are no JaCoCo reports available which can be converted.

    Reason: The JaCoCo plugin was applied to the project, but the generation of XML reports was not activated (which is deactivated by default).

    Possible solution:
      Activate the XML report generation by setting the following property to the 'jacocoTestReport' task:
        tasks.jacocoTestReport {
          reports.xml.required.set(true)
        }
      """.trimIndent())
    }

    return convertibleJacocoReports.flatMap { jacocoReportFile ->
      JacocoXmlReportConverter.convertReport(jacocoReportFile)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}