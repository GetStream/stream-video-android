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
    maven(url = "https://jitpack.io")
    maven(url = "https://plugins.gradle.org/m2/")
  }
}
rootProject.name = "stream-video-android"

// Sample apps
include(":app")
include(":dogfooding")

// SDK
include(":benchmark")
include(":stream-video-android")
include(":stream-video-android-core")
include(":stream-video-android-compose")

// Examples and guide projects
include(":examples:chat-with-video:chat-with-video-final")
include(":examples:chat-with-video:chat-with-video-starter")
