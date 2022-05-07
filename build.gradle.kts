plugins {
  java
  `java-gradle-plugin`
  `maven-publish`
  kotlin("jvm") version "1.6.20"
  id("com.gradle.plugin-publish") version "1.0.0-rc-1"
}

group = "dev.turingcomplete"
version = "1.0.0"

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  api(gradleApi())

  val jUnitVersion = "5.7.1"
  testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-params:$jUnitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

  testImplementation("org.assertj:assertj-core:3.11.1")
}

configure<JavaPluginExtension> {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.getByName<Test>("test") {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    create("bitbucketCodeCoverage") {
      id = "dev.turingcomplete.bitbucket-code-coverage"
      displayName = "Bitbucket Code Coverage Plugin"
      description = "A plugin that provides the capability to publish code coverage to Bitbucket. Currently, only JaCoCo reports are supported."
      implementationClass = "dev.turingcomplete.bitbucketcodecoverage.BitbucketCodeCoveragePlugin"
    }
  }
}

pluginBundle {
  website = "https://github.com/marcelkliemannel/gradle-bitbucket-code-coverage-plugin"
  vcsUrl = "https://github.com/marcelkliemannel/gradle-bitbucket-code-coverage-plugin"
  tags = listOf("test", "code coverage", "bitbucket")
}