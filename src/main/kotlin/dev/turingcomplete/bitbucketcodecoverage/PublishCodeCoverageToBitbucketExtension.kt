package dev.turingcomplete.bitbucketcodecoverage

import dev.turingcomplete.bitbucketcodecoverage.jacoco.PublishJacocoCodeCoverageToBitbucketExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.time.Duration
import javax.inject.Inject

open class PublishCodeCoverageToBitbucketExtension @Inject constructor(objects: ObjectFactory) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val EXTENSION_NAME = "bitbucketCodeCoverage"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  val bitbucketApiHost: Property<String>

  val bitbucketApiUser: Property<String>

  val bitbucketApiPassword: Property<String>

  val bitbucketApiToken: Property<String>

  val bitbucketApiTimeout: Property<Duration>

  val bitbucketCommitId: Property<String>

  val bitbucketProjectKey: Property<String>

  val bitbucketProjectSlug: Property<String>

  val sourceFilesSearchDirs: ConfigurableFileCollection

  val jacoco: PublishJacocoCodeCoverageToBitbucketExtension

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    bitbucketApiHost = objects.property(String::class.java)
    bitbucketApiUser = objects.property(String::class.java)
    bitbucketApiPassword = objects.property(String::class.java)
    bitbucketApiToken = objects.property(String::class.java)
    bitbucketApiTimeout = objects.property(Duration::class.java)
    bitbucketCommitId = objects.property(String::class.java)
    bitbucketProjectKey = objects.property(String::class.java)
    bitbucketProjectSlug = objects.property(String::class.java)
    sourceFilesSearchDirs = objects.fileCollection()
    jacoco = objects.newInstance(PublishJacocoCodeCoverageToBitbucketExtension::class.java)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}