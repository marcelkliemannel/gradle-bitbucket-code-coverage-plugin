import dev.turingcomplete.bitbucketcodecoverage.FileCodeCoverage
import dev.turingcomplete.bitbucketcodecoverage.PublishCodeCoverageToBitbucketExtension
import dev.turingcomplete.bitbucketcodecoverage.PublishCodeCoverageToBitbucketTask
import java.time.Duration

// %placeholderForModifications%

tasks.jacocoTestReport {
  reports.xml.required.set(true)
}