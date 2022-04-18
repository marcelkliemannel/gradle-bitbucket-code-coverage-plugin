package dev.turingcomplete.bitbucketcodecoverage.jacoco

import dev.turingcomplete.bitbucketcodecoverage.ClassCodeCoverage
import dev.turingcomplete.bitbucketcodecoverage.PublishCodeCoverageToBitbucketTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional

open class PublishJacocoCodeCoverageToBitbucketTask : PublishCodeCoverageToBitbucketTask() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val TASK_NAME: String = "publishJacocoCodeCoverageToBitbucket"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  @get:InputFiles
  val jacocoXmlCoverageReports: ConfigurableFileCollection

  @get:[Input Optional]
  val skipOnMissingJacocoXmlCoverageReports: Property<Boolean>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val objects = project.objects
    jacocoXmlCoverageReports = objects.fileCollection()
    skipOnMissingJacocoXmlCoverageReports = objects.property(Boolean::class.java)

    skipOnMissingJacocoXmlCoverageReports.convention(false)
    classCodeCoverages.convention(project.provider { collectClassCodeCoverages() })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun collectClassCodeCoverages(): List<ClassCodeCoverage> {
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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}