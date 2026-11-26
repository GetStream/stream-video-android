@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
        maven("https://stream-io-repo.com")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
        maven("https://stream-io-repo.com")
    }
}

plugins {
    id("com.gradle.enterprise").version("3.17.4")
}
rootProject.name = "stream-video-android"

// Sample apps
include(":demo-app")

// SDK
include(":benchmark")
include(":stream-video-android-core")
include(":stream-video-android-ui-core")
include(":stream-video-android-ui-xml")
include(":stream-video-android-ui-compose")
include(":stream-video-android-filters-video")
include(":stream-video-android-previewdata")
include(":stream-video-android-bom")

// Tutorials
include(":tutorials:tutorial-video")
include(":tutorials:tutorial-audio")
include(":tutorials:tutorial-livestream")
include(":tutorials:tutorial-ringing")

// Support
include(":metrics:stream-video-android-metrics")

buildCache {
    local {
        isEnabled = !System.getenv().containsKey("CI") ||
            System.getenv("GRADLE_LOCAL_BUILD_CACHE") == "true"
        removeUnusedEntriesAfterDays = 7
    }
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
