package dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper

import java.io.File


object TestProject {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private const val TEST_PROJECT_FILES_BASE_DIR = "/dev/turingcomplete/gradlebitbucketcodecoverageplugin/testproject/"

  private val TEST_PROJECT_FILES = listOf("settings.gradle.kts",
                                          "build.gradle.kts",
                                          "sub-project-1/build.gradle.kts",
                                          "sub-project-1/src/main/java/firstPackage/secondPackage/ClassWithPackage.java",
                                          "sub-project-1/src/main/java/ClassWithoutPackage.java",
                                          "sub-project-1/src/test/java/CoverageTest.java",
                                          "sub-project-2/build.gradle.kts",
                                          "sub-project-2/src/main/java/ClassInSecondProject.java")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createTestProject(projectRootDir: File) {
    TEST_PROJECT_FILES.forEach { testProjectFile ->
      File(projectRootDir, testProjectFile).also { it.parentFile.mkdirs(); it.createNewFile() }.outputStream().use {
        TestProject::class.java.getResourceAsStream(TEST_PROJECT_FILES_BASE_DIR + testProjectFile)?.copyTo(it) ?: throw IllegalStateException("Can't find test project file: $testProjectFile")
      }
    }
  }

  fun configureSubProjects(projectRootDir: File, configuration: String) {
    val buildGradleKts = File(projectRootDir, "build.gradle.kts")
    buildGradleKts.writeText(buildGradleKts.readText().replace("// %placeholderForModifications%", configuration))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}