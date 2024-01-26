plugins {
  java
  `java-gradle-plugin`
  `maven-publish`
  kotlin("jvm") version "1.6.20"
  id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.turingcomplete"
version = "1.0.1"

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
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.getByName<Test>("test") {
  useJUnitPlatform()
  val tmpDir = File(buildDir, "tmp/test-tmp")
  systemProperty("java.io.tmpdir", tmpDir)
  doFirst {
    tmpDir.mkdirs()
  }
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