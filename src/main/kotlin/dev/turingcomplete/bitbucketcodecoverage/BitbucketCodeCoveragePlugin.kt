@file:Suppress("unused") // Referenced in the root build.gradle.kts

package dev.turingcomplete.bitbucketcodecoverage

import dev.turingcomplete.bitbucketcodecoverage.jacoco.PublishJacocoCodeCoverageToBitbucketTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class BitbucketCodeCoveragePlugin : Plugin<Project> {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    fun configureBitbucketApiPropertiesFromExtension(task: PublishCodeCoverageToBitbucketTask,
                                                     extension: PublishCodeCoverageToBitbucketExtension) {

      task.bitbucketApiHost.set(extension.bitbucketApiHost)
      task.bitbucketApiUser.set(extension.bitbucketApiUser)
      task.bitbucketApiPassword.set(extension.bitbucketApiPassword)
      task.bitbucketApiToken.set(extension.bitbucketApiToken)
      task.bitbucketApiTimeout.set(extension.bitbucketApiTimeout)
      task.bitbucketCommitId.set(extension.bitbucketCommitId)
      task.bitbucketProjectKey.set(extension.bitbucketProjectKey)
      task.bitbucketRepositorySlug.set(extension.bitbucketRepositorySlug)
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun apply(project: Project) {
    val extension = project.extensions.create(PublishCodeCoverageToBitbucketExtension.EXTENSION_NAME, PublishCodeCoverageToBitbucketExtension::class.java)

    project.afterEvaluate {
      if (project.pluginManager.hasPlugin("org.gradle.jacoco")) {
        configurePublishJacocoCodeCoverageToBitbucketTask(project, extension)
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun configurePublishJacocoCodeCoverageToBitbucketTask(project: Project, extension: PublishCodeCoverageToBitbucketExtension) {
    project.tasks.register(PublishJacocoCodeCoverageToBitbucketTask.TASK_NAME, PublishJacocoCodeCoverageToBitbucketTask::class.java) {
      // Configure PublishCodeCoverageToBitbucketTask
      configureBitbucketApiPropertiesFromExtension(it, extension)
      configureSourceFilesSearchDirs(it, extension, project)

      // Configure PublishJacocoCodeCoverageToBitbucketTask
      val jacocoReportTasks = project.tasks.withType(JacocoReport::class.java)
      it.dependsOn(jacocoReportTasks)
      it.onlyIf { jacocoReportTasks.any { task -> task.didWork } }

      it.skipOnMissingJacocoXmlCoverageReports.set(extension.jacoco.skipOnMissingJacocoXmlCoverageReports)

      if (extension.jacoco.jacocoXmlCoverageReports.isEmpty) {
        it.jacocoXmlCoverageReports.builtBy(jacocoReportTasks)
        it.jacocoXmlCoverageReports.from(jacocoReportTasks.map { it.outputs }.toTypedArray())
      }
    }
  }

  private fun configureSourceFilesSearchDirs(task: PublishCodeCoverageToBitbucketTask,
                                             extension: PublishCodeCoverageToBitbucketExtension,
                                             project: Project) {

    task.sourceFilesSearchDirs.setFrom(extension.sourceFilesSearchDirs)

    if (task.sourceFilesSearchDirs.isEmpty) {
      val foo = project.extensions.findByType(JavaPluginExtension::class.java)?.let { javaPluginExtension ->
        project.files(javaPluginExtension.sourceSets.flatMap { sourceSet -> sourceSet.allSource.sourceDirectories }.toList())
      }
      task.sourceFilesSearchDirs.from(foo)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}