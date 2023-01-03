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
include(":app")
include(":benchmark")
include(":dogfooding")
include(":stream-video-android")
include(":stream-video-android-ui-common")
include(":stream-video-android-xml")
include(":stream-video-android-compose")
include(":stream-video-android-pushprovider-firebase")
include(":examples:chat-with-video-sample")
