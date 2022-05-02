package dev.turingcomplete.bitbucketcodecoverage

import dev.turingcomplete.bitbucketcodecoverage.jacoco.PublishJacocoCodeCoverageToBitbucketExtension
import dev.turingcomplete.bitbucketcodecoverage.jacoco.PublishJacocoCodeCoverageToBitbucketTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.time.Duration
import javax.inject.Inject

/**
 * Extension to configure a [PublishCodeCoverageToBitbucketTask].
 */
open class PublishCodeCoverageToBitbucketExtension @Inject constructor(objects: ObjectFactory) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val EXTENSION_NAME = "bitbucketCodeCoverage"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  /**
   * The host address of Bitbucket (e.g., `https://mybitbucket.com`).
   *
   * Must start with `http://` or `https://`.
   */
  val bitbucketHost: Property<String>

  /**
   * A Bitbucket user for the authentication.
   *
   * Must be set in conjunction with [bitbucketPassword].
   */
  val bitbucketUser: Property<String>

  /**
   * The password of the [bitbucketUser].
   *
   * Must be set in conjunction with [bitbucketUser].
   */
  val bitbucketPassword: Property<String>

  /**
   * A token to use for authentication as an alternative to the user/password
   * authentication.
   *
   * The value will be ignored if [bitbucketUser] is set.
   */
  val bitbucketToken: Property<String>

  /**
   * The timeout of any request to Bitbucket. The default value is 30 seconds.
   */
  val bitbucketTimeout: Property<Duration>

  /**
   * The Git commit ID to which the published code coverage should be associated.
   *
   * If [bitbucketProjectKey] and [bitbucketRepositorySlug] is not set,
   * Bitbucket will add the code coverage to all repositories which have a
   * commit with this ID.
   */
  val bitbucketCommitId: Property<String>

  /**
   * A Bitbucket project key.
   *
   * Must be set in conjunction with [bitbucketRepositorySlug].
   */
  val bitbucketProjectKey: Property<String>

  /**
   * A repository slug.
   *
   * Must be set in conjunction with [bitbucketProjectKey].
   */
  val bitbucketRepositorySlug: Property<String>

  /**
   * An instance of the [PublishJacocoCodeCoverageToBitbucketExtension] to
   * configure the [PublishJacocoCodeCoverageToBitbucketTask].
   */
  val jacoco: PublishJacocoCodeCoverageToBitbucketExtension

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    bitbucketHost = objects.property(String::class.java)
    bitbucketUser = objects.property(String::class.java)
    bitbucketPassword = objects.property(String::class.java)
    bitbucketToken = objects.property(String::class.java)
    bitbucketTimeout = objects.property(Duration::class.java)
    bitbucketCommitId = objects.property(String::class.java)
    bitbucketProjectKey = objects.property(String::class.java)
    bitbucketRepositorySlug = objects.property(String::class.java)
    jacoco = objects.newInstance(PublishJacocoCodeCoverageToBitbucketExtension::class.java)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}