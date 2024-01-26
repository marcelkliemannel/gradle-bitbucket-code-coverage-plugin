package dev.turingcomplete.gradlebitbucketcodecoverageplugin

import dev.turingcomplete.bitbucketcodecoverage.FileCodeCoverage
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.MockBitbucketServer
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.SUB_PROJECT_1_NAME
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.SUB_PROJECT_2_NAME
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.TEST_PUBLISH_CODE_COVERAGE_TO_BITBUCKET_TASK
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.configureRootProject
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.configureSubProject1
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.configureSubProject2
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.createTestProject
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.createTestPublishCodeCoverageToBitbucketTask
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.util.*

class PublishCodeCoverageToBitbucketTaskTest {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @TempDir
  lateinit var testProjectDir: File

  private lateinit var gradleRunner: GradleRunner
  private lateinit var mockBitbucketServer: MockBitbucketServer

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  @BeforeEach
  fun setUp() {
    createTestProject(testProjectDir)

    gradleRunner = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withTestKitDir(File(testProjectDir, "testKitDir"))

    mockBitbucketServer = MockBitbucketServer()
  }

  @AfterEach
  fun cleanUp() {
    mockBitbucketServer.stop()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Test
  fun `Test usage of project key and repository slug in request path`() {
    configureRootProject(testProjectDir, """
      bitbucketCodeCoverage {
        bitbucketHost.set("${mockBitbucketServer.getHost()}")
        bitbucketTimeout.set(Duration.ofSeconds(5))
        bitbucketCommitId.set("98sjrmx")
        bitbucketProjectKey.set("PROJECT_KEY")
        bitbucketRepositorySlug.set("project_slug")
      }
      
      ${createTestPublishCodeCoverageToBitbucketTask(listOf(FileCodeCoverage(File("Foo.java"), setOf(), setOf(), setOf())))}
    """.trimIndent())

    gradleRunner
            .withArguments(TEST_PUBLISH_CODE_COVERAGE_TO_BITBUCKET_TASK, "--stacktrace")
            .forwardOutput()
            .build()

    val actualRequests = mockBitbucketServer.getReceivedRequests()
    assertThat(actualRequests.map { it.requestPath }).allMatch { it == "/rest/code-coverage/1.0/projects/PROJECT_KEY/repos/project_slug/commits/98sjrmx" }
  }

  @Test
  fun `Test authorization header values`() {
    val user = "bitbucketUser123"
    val password = "bitbucketPassword$$"
    configureSubProject1(testProjectDir, """
      bitbucketCodeCoverage {
        bitbucketHost.set("${mockBitbucketServer.getHost()}")
        bitbucketTimeout.set(Duration.ofSeconds(5))
        bitbucketCommitId.set("dummyCommitId")
        bitbucketUser.set("$user")
        bitbucketPassword.set("$password")
        // If user and password was set, the token should be ignored. 
        bitbucketToken.set("shouldBeIgnore")
      }
      
      ${createTestPublishCodeCoverageToBitbucketTask(listOf(FileCodeCoverage(File("Foo.java"), setOf(), setOf(), setOf())))}
    """.trimIndent())

    val token = """bitbucketToken&&"""
    configureSubProject2(testProjectDir, """
      bitbucketCodeCoverage {
        bitbucketHost.set("${mockBitbucketServer.getHost()}")
        bitbucketTimeout.set(Duration.ofSeconds(5))
        bitbucketCommitId.set("dummyCommitId")
        bitbucketToken.set("$token")
      }
      
      ${createTestPublishCodeCoverageToBitbucketTask(listOf(FileCodeCoverage(File("Foo.java"), setOf(), setOf(), setOf())))}
    """.trimIndent())

    gradleRunner
            .withArguments(":$SUB_PROJECT_1_NAME:$TEST_PUBLISH_CODE_COVERAGE_TO_BITBUCKET_TASK",
                           ":$SUB_PROJECT_2_NAME:$TEST_PUBLISH_CODE_COVERAGE_TO_BITBUCKET_TASK",
                           "--stacktrace")
            .forwardOutput()
            .build()

    val actualRequests = mockBitbucketServer.getReceivedRequests()
    assertThat(actualRequests.map { it.authorizationHeader })
            .containsExactlyInAnyOrder("Basic ${Base64.getEncoder().encodeToString("$user:$password".encodeToByteArray())}",
                                       "Bearer $token")
  }

  @ParameterizedTest
  @CsvSource(value = [
    "|||C:;P:;U:|false",
    "1|2|3|C:1;P:2;U:3|true",
    "1,2|3,4,5|6,7,8,9|C:1,2;P:3,4,5;U:6,7,8,9|true"
  ], delimiter = '|')
  fun `Test toBitbucketCodeCoverage`(fullyCoveredLines: String?,
                                     partiallyCoveredLines: String?,
                                     uncoveredCoveredLines: String?,
                                     expectedBitbucketCoverage: String,
                                     expectedHasCoverageInfo: Boolean) {
    val coverage = FileCodeCoverage(
            File(""),
            fullyCoveredLines?.split(",")?.map { it.toInt() }?.toSet() ?: emptySet(),
            partiallyCoveredLines?.split(",")?.map { it.toInt() }?.toSet() ?: emptySet(),
            uncoveredCoveredLines?.split(",")?.map { it.toInt() }?.toSet() ?: emptySet())

    assertThat(coverage.toBitbucketCodeCoverage()).isEqualTo(expectedBitbucketCoverage)
    assertThat(coverage.hasCoverageInfo()).isEqualTo(expectedHasCoverageInfo)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}