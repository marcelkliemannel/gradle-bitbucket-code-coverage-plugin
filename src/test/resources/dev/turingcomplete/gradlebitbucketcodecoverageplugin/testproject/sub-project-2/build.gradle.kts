import java.time.Duration

plugins {
  kotlin("jvm") version "1.6.20"
}

// %placeholderForModifications%

tasks.jacocoTestReport {
  reports.xml.required.set(true)
}