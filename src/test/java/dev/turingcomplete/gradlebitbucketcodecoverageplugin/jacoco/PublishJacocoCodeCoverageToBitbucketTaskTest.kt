package dev.turingcomplete.gradlebitbucketcodecoverageplugin.jacoco

import dev.turingcomplete.bitbucketcodecoverage.jacoco.PublishJacocoCodeCoverageToBitbucketTask
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.MockBitbucketServer
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.configureSubProjects
import dev.turingcomplete.gradlebitbucketcodecoverageplugin.__helper.TestProject.createTestProject
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PublishJacocoCodeCoverageToBitbucketTaskTest {
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
  fun `Test single project code coverage send to Bitbucket API`() {
    configureSubProjects(testProjectDir, """
      bitbucketCodeCoverage {
        bitbucketApiHost.set("${mockBitbucketServer.getHost()}")
        bitbucketApiTimeout.set(Duration.ofSeconds(5))
        bitbucketCommitId.set("12345")
      }
      
      tasks.jacocoTestReport {
        reports.xml.required.set(true)
      }
    """.trimIndent())

    val result = gradleRunner
            .withArguments(PublishJacocoCodeCoverageToBitbucketTask.TASK_NAME, "--stacktrace")
            .forwardOutput()
            .build()

    val receivedRequest = mockBitbucketServer.getSingleReceivedRequest()
    assertThat(receivedRequest.requestPath).isEqualTo("/bitbucket/rest/code-coverage/1.0/commits/12345")
    assertThat(receivedRequest.requestMethod).isEqualTo("POST")
    assertThat(receivedRequest.requestBody).isEqualTo("""
      {
        "files": [
          {
            "path": "sub-project-1/src/main/java/ClassWithoutPackage.java",
            "coverage": "C:5,6,8,9,10,12,13,14,18,23,31,36,38,40,41,44,46,47;P:17;U:21,26,27,28,33,34"
          },
          {
            "path": "sub-project-1/src/main/java/firstPackage/secondPackage/ClassWithPackage.java",
            "coverage": "C:7,8,10,11,12,15,16,17,21,26,34,39,41,43,44;P:20;U:24,29,30,31,36,37,47,49,50"
          }
        ]
      }
    """.trimIndent())

    assertThat(result.task(":sub-project-1:${PublishJacocoCodeCoverageToBitbucketTask.TASK_NAME}")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}