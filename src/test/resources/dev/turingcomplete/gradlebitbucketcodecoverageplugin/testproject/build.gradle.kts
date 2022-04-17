import java.time.Duration

plugins {
  java
  id("dev.turingcomplete.bitbucket-code-coverage") version "1.0.0"
  jacoco
}

group = "dev.turingcomplete"
version = "1.0.0"

subprojects {
  apply(plugin = "java")
  apply(plugin = "jacoco")
  apply(plugin = "dev.turingcomplete.bitbucket-code-coverage")

  repositories {
    mavenCentral()
  }

  dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  }

  tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
  }

  tasks.jacocoTestReport {
    dependsOn(tasks.test)
  }

  bitbucketCodeCoverage {
    bitbucketApiHost.set("http://localhost")
  }

  // %placeholderForModifications%
}