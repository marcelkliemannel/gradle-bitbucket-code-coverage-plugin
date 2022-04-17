plugins {
  java
  `java-gradle-plugin`
  kotlin("jvm") version "1.6.20"
  `maven-publish`
}

group = "dev.turingcomplete"
version = "1.0.0"

repositories {
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

tasks.getByName<Test>("test") {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    create("bitbucketCodeCoverage") {
      id = "dev.turingcomplete.bitbucket-code-coverage"
      implementationClass = "dev.turingcomplete.bitbucketcodecoverage.BitbucketCodeCoveragePlugin"
    }
  }
}