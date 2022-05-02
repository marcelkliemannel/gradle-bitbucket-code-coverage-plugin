@file:Suppress("unused") // Referenced in the root build.gradle.kts

package dev.turingcomplete.bitbucketcodecoverage

import dev.turingcomplete.bitbucketcodecoverage.jacoco.PublishJacocoCodeCoverageToBitbucketTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.tasks.JacocoReport

class BitbucketCodeCoveragePlugin : Plugin<Project> {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  /**
   * Adds the extension [PublishCodeCoverageToBitbucketExtension] with the name
   * [PublishCodeCoverageToBitbucketExtension.EXTENSION_NAME] to the given
   * `project`.
   *
   * If the JaCoCo plugin (ID: `org.gradle.jacoco`) was applied to the project,
   * a default [PublishJacocoCodeCoverageToBitbucketTask] will be added.
   *
   * @see addPublishJacocoCodeCoverageToBitbucketTask
   */
  override fun apply(project: Project) {
    val extension = project.extensions.create(PublishCodeCoverageToBitbucketExtension.EXTENSION_NAME, PublishCodeCoverageToBitbucketExtension::class.java)

    project.afterEvaluate {
      if (project.pluginManager.hasPlugin("org.gradle.jacoco")) {
        addPublishJacocoCodeCoverageToBitbucketTask(project, extension)
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  /**
   * Creates the default [PublishJacocoCodeCoverageToBitbucketTask] with the
   * name [PublishJacocoCodeCoverageToBitbucketTask.TASK_NAME] and ads it to
   * the group [LifecycleBasePlugin.VERIFICATION_GROUP].
   *
   * This tasks gets bound to the outputs of all [JacocoReport] tasks and configures
   * the [PublishJacocoCodeCoverageToBitbucketTask.sourceFilesSearchDirs] to
   * all source directories of this project.
   */
  private fun addPublishJacocoCodeCoverageToBitbucketTask(project: Project, extension: PublishCodeCoverageToBitbucketExtension) {
    project.tasks.register(PublishJacocoCodeCoverageToBitbucketTask.TASK_NAME, PublishJacocoCodeCoverageToBitbucketTask::class.java) { task ->
      task.group = LifecycleBasePlugin.VERIFICATION_GROUP

      task.configureFromExtension(extension)

      // Make this task dependent on the `JacocoReport` task.
      val jacocoReportTasks = project.tasks.withType(JacocoReport::class.java)
      task.dependsOn(jacocoReportTasks)
      task.onlyIf { jacocoReportTasks.any { task -> task.didWork } }

      // Set the default JaCoCo XML reports files to the output of the
      // `JacocoReport` tasks.
      if (extension.jacoco.jacocoXmlCoverageReports.isEmpty) {
        task.jacocoXmlCoverageReports.builtBy(jacocoReportTasks)
        task.jacocoXmlCoverageReports.from(jacocoReportTasks.map { it.outputs }.toTypedArray())
      }

      // Set the default source files search dirs to all source directories of
      // this project.
      val allSourceDirs = project.extensions.findByType(JavaPluginExtension::class.java)?.sourceSets?.flatMap { sourceSet ->
        sourceSet.allSource.sourceDirectories
      }?.toList()
      task.sourceFilesSearchDirs.from(allSourceDirs)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}