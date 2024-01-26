package dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper

import dev.turingcomplete.bitbucketcodecoverage.FileCodeCoverage
import java.io.File


object TestProject {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private const val BUILD_GRADLE_PLACEHOLDER = "// %placeholderForModifications%"
  const val TEST_PUBLISH_CODE_COVERAGE_TO_BITBUCKET_TASK = "testPublishCodeCoverageToBitbucketTask"
  const val SUB_PROJECT_1_NAME = "sub-project-1"
  const val SUB_PROJECT_2_NAME = "sub-project-2"
  val SUB_PROJECT_NAMES = listOf(SUB_PROJECT_1_NAME, SUB_PROJECT_2_NAME)

  private const val TEST_PROJECT_FILES_BASE_DIR = "/dev/turingcomplete/gradlebitbucketcodecoverageplugin/testproject/"

  private val TEST_PROJECT_FILES = listOf("settings.gradle.kts",
                                          "build.gradle.kts",
                                          "$SUB_PROJECT_1_NAME/build.gradle.kts",
                                          "$SUB_PROJECT_1_NAME/src/main/java/firstPackage/secondPackage/ClassWithPackage.java",
                                          "$SUB_PROJECT_1_NAME/src/main/java/firstPackage/secondPackage/InterfaceWithoutDefaults.java",
                                          "$SUB_PROJECT_1_NAME/src/main/java/ClassWithoutPackage.java",
                                          "$SUB_PROJECT_1_NAME/src/test/java/FirstProjectCoverageTest.java",
                                          "$SUB_PROJECT_2_NAME/build.gradle.kts",
                                          "$SUB_PROJECT_2_NAME/src/main/kotlin/KotlinClass.kt",
                                          "$SUB_PROJECT_2_NAME/src/test/kotlin/SecondProjectCoverageTest.kt")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createTestProject(projectRootDir: File) {
    TEST_PROJECT_FILES.forEach { testProjectFile ->
      File(projectRootDir, testProjectFile).also { it.parentFile.mkdirs(); it.createNewFile() }.outputStream().use {
        TestProject::class.java.getResourceAsStream(TEST_PROJECT_FILES_BASE_DIR + testProjectFile)?.copyTo(it) ?: throw IllegalStateException("Can't find test project file: $testProjectFile")
      }
    }
  }

  fun configureRootProject(projectRootDir: File, configuration: String) {
    configureProject(projectRootDir, configuration)
  }

  fun configureSubProjects(projectRootDir: File, configuration: String) {
    for (subProjectName in SUB_PROJECT_NAMES) {
      configureProject(File(projectRootDir, subProjectName), configuration)
    }
  }

  fun configureSubProject1(projectRootDir: File, configuration: String) {
    configureProject(File(projectRootDir, SUB_PROJECT_1_NAME), configuration)
  }

  fun configureSubProject2(projectRootDir: File, configuration: String) {
    configureProject(File(projectRootDir, SUB_PROJECT_2_NAME), configuration)
  }

  fun createTestPublishCodeCoverageToBitbucketTask(fileCodeCoverages: List<FileCodeCoverage>) = """
    tasks.register<PublishCodeCoverageToBitbucketTask>("testPublishCodeCoverageToBitbucketTask") {
      fileCodeCoverages.set(listOf(${fileCodeCoverages.joinToString { "FileCodeCoverage(File(\"${it.sourceFile}\"), setOf(${it.fullyCoveredLines.joinToString()}), setOf(${it.partiallyCoveredLines.joinToString()}), setOf(${it.uncoveredLines.joinToString()}))" }}))

      val extension = project.extensions.findByType(PublishCodeCoverageToBitbucketExtension::class.java)!!
      configureFromExtension(extension)
    }
  """.trimIndent()

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun configureProject(projectDir: File, configuration: String) {
    val buildGradleKts = File(projectDir, "build.gradle.kts")
    buildGradleKts.writeText(buildGradleKts.readText().replace(BUILD_GRADLE_PLACEHOLDER, "$BUILD_GRADLE_PLACEHOLDER\n$configuration"))
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}