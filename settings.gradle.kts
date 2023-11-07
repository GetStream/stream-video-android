@file:Suppress("UnstableApiUsage")

import com.github.burrunan.s3cache.AwsS3BuildCache

pluginManagement {
    includeBuild("build-logic")
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

plugins {
    id("com.gradle.enterprise").version("3.13.3")
    id("com.github.burrunan.s3-build-cache").version("1.2")
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
include(":stream-video-android-mock")
include(":stream-video-android-bom")

// Tutorials
include(":tutorials:tutorial-video")
include(":tutorials:tutorial-audio")
include(":tutorials:tutorial-livestream")

buildCache {
    local {
        isEnabled = !System.getenv().containsKey("CI")
        removeUnusedEntriesAfterDays = 7
    }
    val localProperties = java.util.Properties()
    val file = File("local.properties")
    if (file.exists()) {
        file.inputStream().use { localProperties.load(it) }
    }
    val streamAWSRegion = (localProperties["buildCache.AWSRegion"] as? String)
        ?: System.getenv("BUILD_CACHE_AWS_REGION") ?: ""
    val streamAWSBucket = (localProperties["buildCache.AWSBucket"] as? String)
        ?: System.getenv("BUILD_CACHE_AWS_BUCKET") ?: ""
    val streamAWSAccessKeyId = (localProperties["buildCache.AWSAccessKeyId"] as? String)
        ?: System.getenv("BUILD_CACHE_AWS_ACCESS_KEY_ID") ?: ""
    val streamAWSSecretKey = (localProperties["buildCache.AWSSecretKey"] as? String)
        ?: System.getenv("BUILD_CACHE_AWS_SECRET_KEY") ?: ""
    val streamBuildCacheDisabled =
        (localProperties.get("buildCache.disabled") as? String).toBoolean()
    if (!streamBuildCacheDisabled &&
        streamAWSRegion.isNotBlank() &&
        streamAWSBucket.isNotBlank() &&
        streamAWSAccessKeyId.isNotBlank() &&
        streamAWSSecretKey.isNotBlank()
    ) {
        remote(AwsS3BuildCache::class.java) {
            region = streamAWSRegion
            bucket = streamAWSBucket
            prefix = "cache/"
            awsAccessKeyId = streamAWSAccessKeyId
            awsSecretKey = streamAWSSecretKey
            isPush = System.getenv().containsKey("CI")
        }
    }
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}