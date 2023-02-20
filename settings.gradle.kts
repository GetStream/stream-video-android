@file:Suppress("UnstableApiUsage")
pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
  }
}
rootProject.name = "stream-video-android"

// Sample apps
include(":app")
include(":dogfooding")

// SDK
include(":benchmark")
include(":stream-video-android-core")
include(":stream-video-android-ui-common")
include(":stream-video-android-xml")
include(":stream-video-android-compose")

// Examples and guide projects
include(":examples:tutorial:tutorial-starter")
include(":examples:tutorial:tutorial-final")

include(":examples:chat-with-video:chat-with-video-final")
include(":examples:chat-with-video:chat-with-video-starter")
