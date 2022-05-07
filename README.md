# Gradle Bitbucket Code Coverage Plugin

This plugin provides the capability to publish code coverage to Bitbucket. It communicates with the endpoints provided by the Bitbucket plugin: [Code Coverage for Bitbucket Server](https://marketplace.atlassian.com/apps/1218271/code-coverage-for-bitbucket-server).

**Currently, the plugin only supports the conversion of** [**JaCoCo**](https://docs.gradle.org/current/userguide/jacoco_plugin.html) **reports.** But thanks to its modular design, it's easily possible to [extend the plugin to other report types](#extend-the-plugin-to-other-report-types).

## Apply Plugin

__The plugin is available in the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.turingcomplete.bitbucket-code-coverage).__

Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):
```kotlin
plugins { 
    id("dev.turingcomplete.bitbucket-code-coverage") version "1.0.0"
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
```kotlin
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("dev.turingcomplete.bitbucket-code-coverage:1.0.0")
    }
}

apply(plugin = "dev.turingcomplete.bitbucket-code-coverage")
```

️️⚠️ The minimum supported Gradle version is 7.1.

## Minimal Setup for JaCoCo Code Coverage

The following code must be present at least in the `build.gradle.kts` for the plugin to publish JaCoCo code coverage reports:

```kotlin
plugins {
    jacoco
    id("dev.turingcomplete.bitbucket-code-coverage") version "1.0.0"
}

bitbucketCodeCoverage {
    bitbucketHost.set("https://mybitbucket.com")
    bitbucketCommitId.set("0835f844bc4950d7...")
}

tasks.jacocoTestReport {
    reports.xml.required.set(true)
}
```

- Line 2: Apply the `jacoco` plugin to the project.
- Line 6: Set the host address to the Bitbucket instance.
- Line 7: Set a Git commit ID, to which Bitbucket will associate the published code coverage.
- Line 10-12: Configure the `jacocoTestReport` task to produce XML report files. The plugin will convert these into the Bitbucket code coverage format.

## Tasks

### Publish JaCoCo Code Coverage to Bitbucket

The `PublishJacocoCodeCoverageToBitbucketTask` class provides the functionality to convert JaCoCo XML report files into the Bitbucket code coverage format and can then publish them to Bitbucket.

If the project has the `jacoco` plugin applied, this plugin will add 
a default instance of the `PublishJacocoCodeCoverageToBitbucketTask`  with the name `publishJacocoCodeCoverageToBitbucket` under the group `verification`.

This default task depends on all `JacocoReport` tasks (e.g., `jacocoTestReport`) and uses their outputs as inputs and will only run if at least one of these tasks is not up-to-date. In addition, the `sourceFilesSearchDirs` property will be mapped to all source directories of all `SourceSets` in the projects.

#### Properties

The `PublishJacocoCodeCoverageToBitbucketTask` has the following properties:

| Property                                | Required | Description                                                                                                                                                                                                                                               |
|-----------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `bitbucketHost`                         | Yes      | The host address to the Bitbucket instance. Must start with `http://` or `https://`.                                                                                                                                                                      |
| `bitbucketCommitId`                     | Yes      | The Git commit ID to which Bitbucket will associate the published code coverage.                                                                                                                                                                          |
| `bitbucketProjectKey`                   | No       | The key of a Bitbucket project.                                                                                                                                                                                                                           |
| `bitbucketRepositorySlug`               | No       | The slug of a repository in a Bitbucket project.                                                                                                                                                                                                          |
| `bitbucketUser`                         | No       | The name of a Bitbucket user.                                                                                                                                                                                                                             |
| `bitbucketPassword`                     | No       | The password of the `bitbucketUser`.                                                                                                                                                                                                                      |
| `bitbucketToken`                        | No       | An [HTTP access token](https://confluence.atlassian.com/bitbucketserver/http-access-tokens-939515499.html) to use for authentication as an alternative to the user/password authentication. (The task will ignore the value if a `bitbucketUser` is set.) |
| `bitbucketTimeout`                      | No       | The timeout of a request to Bitbucket. The default value is 30 seconds.                                                                                                                                                                                   |
| `jacocoXmlCoverageReports`              | No       | A `FileCollection` of JaCoCo XML report files.                                                                                                                                                                                                            |
| `skipOnMissingJacocoXmlCoverageReports` | No       | If set to `true` and `jacocoXmlCoverageReports` is empty, the task will not fail.                                                                                                                                                                         |
| `sourceFilesSearchDirs`                 | No       | Bitbucket requires file paths relative to the root project directory, but the JaCoCo reports only contain the fully qualified class name. Therefore, the task needs to search in the given directories for the source files of all classes.               |

If we only set the Git commit ID via the property `bitbucketCommitId`, the code coverage would be associated with all commits in *all* repositories that have the given ID. The property `bitbucketProjectKey` and `bitbucketRepositorySlug` can be specified to limit the scope of the code coverage to a commit in one repository.

## Extension

The default task `publishJacocoCodeCoverageToBitbucket` can be configured via the extension `bitbucketCodeCoverage`. This extension provides the same properties as the task (the JaCoCo specific ones are nested under the sub extension `jacoco`).

The following code shows an example of a wholly configured extension:

```kotlin
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

bitbucketCodeCoverage {
  bitbucketHost.set("https://mybitbucket.com") // Required

  bitbucketCommitId.set("0835f844bc4950d7...") // Required
  bitbucketProjectKey.set("PROJECT_KEY") // Optional
  bitbucketRepositorySlug.set("repositorySlug") // Optional

  bitbucketUser.set("User") // Optional
  bitbucketPassword.set("Password$") // Optional
  bitbucketToken.set("token1234") // Optional; alternative to the user/password

  bitbucketTimeout.set(Duration.of(1, ChronoUnit.MINUTES)) // Optional

  jacoco {
    jacocoXmlCoverageReports.setFrom(project.files(File("report.xml"))) // Optional
    skipOnMissingJacocoXmlCoverageReports.set(true) // Optional
    sourceFilesSearchDirs.setFrom(project.files(File("alternate", "search-dir"))) // Optional
  }
}
```

## Appendix

### Automatically Obtain the Git Commit ID

It's easy to automatically obtain the current Git commit ID and set it to the task property. One way would be to use the [JGit](https://www.eclipse.org/jgit/) library:

```kotlin
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r")
  }
}

bitbucketCodeCoverage {
  bitbucketCommitId.set(provider { Git.open(rootProject.projectDir).repository.resolve(Constants.HEAD).name })
  // ...
}
```

Alternatively, without a dependency, it would be possible to execute the command `git rev-parse HEAD` via a `ProcessBuilder` and get the Git commit ID from the output of this process.

### Extend the Plugin to Other Report Types

We need to create a new task to convert other code coverage reports besides the JaCoCo ones that extend `PublishCodeCoverageToBitbucketTask`. This new task must populate the property `fileCodeCoverages` with instances of `FileCodeCoverage`. These contain the code coverage information of a single source code file. 

An example task could look as follows:

```kotlin
class CustomPublishCodeCoverageToBitbucketTask : PublishCodeCoverageToBitbucketTask() {

  init {
    val myClassCodeCoverage = FileCodeCoverage(sourceFile = File("src/main/java/MyClass.java"), 
                                               fullyCoveredLines = setOf(1, 2), 
                                               partiallyCoveredLines = setOf(3, 4),
                                               uncoveredLines = setOf(5, 6))
    fileCodeCoverages.set(listOf(myClassCodeCoverage))
  }
}
```

## Licensing

Copyright (c) 2022 Marcel Kliemannel

Licensed under the **Apache License, Version 2.0** (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at <https://www.apache.org/licenses/LICENSE-2.0>.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License.
