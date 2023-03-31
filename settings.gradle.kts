@file:Suppress("UnstableApiUsage")
pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    // we should remove this after releasing the stable version of Stream Result
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
    // we should remove this after releasing the stable version of Stream Result
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
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
include(":stream-video-android-tooling")
include(":stream-video-android-bom")

// Examples and guide projects
include(":examples:tutorial:tutorial-starter")
include(":examples:tutorial:tutorial-final")

include(":examples:chat-with-video:chat-with-video-final")
include(":examples:chat-with-video:chat-with-video-starter")
