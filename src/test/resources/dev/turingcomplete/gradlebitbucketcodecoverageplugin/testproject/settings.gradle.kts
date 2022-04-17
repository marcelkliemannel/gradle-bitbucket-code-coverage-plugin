rootProject.name = "test-project"

pluginManagement {
  repositories {
    //mavenLocal()
    gradlePluginPortal()
  }
}

include("sub-project-1", "sub-project-2")