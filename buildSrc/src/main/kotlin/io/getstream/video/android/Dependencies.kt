package io.getstream.video.android

object Versions {
    internal const val ANDROID_GRADLE_PLUGIN = "7.2.1"
    internal const val ANDROID_GRADLE_SPOTLESS = "6.7.0"
    internal const val GRADLE_NEXUS_PUBLISH_PLUGIN = "1.1.0"
    internal const val KOTLIN = "1.6.10"
    internal const val KOTLIN_GRADLE_DOKKA = "1.6.10"
    internal const val KOTLIN_SERIALIZATION_JSON = "1.3.2"
    internal const val KOTLIN_BINARY_VALIDATOR = "0.11.0"
    internal const val KOTLIN_COROUTINE = "1.6.4"

    internal const val MATERIAL = "1.6.1"
    internal const val ANDROIDX_APPCOMPAT = "1.4.2"
    internal const val ANDROIDX_CORE = "1.8.0"
    internal const val ANDROIDX_LIFECYCLE = "2.5.0"
    internal const val ACTIVITY_COMPOSE = "1.5.0"
    internal const val COMPOSE = "1.1.1"

    internal const val WIRE = "4.4.0"
    internal const val RETROFIT = "2.9.0"
    internal const val OKHTTP = "4.10.0"

    internal const val LIVE_KIT = "1.0.1"
}

object Dependencies {
    const val androidGradlePlugin =
        "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}"
    const val gradleNexusPublishPlugin =
        "io.github.gradle-nexus:publish-plugin:${Versions.GRADLE_NEXUS_PUBLISH_PLUGIN}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val kotlinSerializationPlugin = "org.jetbrains.kotlin:kotlin-serialization:${Versions.KOTLIN}"
    const val kotlinSerializationJson =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLIN_SERIALIZATION_JSON}"
    const val spotlessGradlePlugin =
        "com.diffplug.spotless:spotless-plugin-gradle:${Versions.ANDROID_GRADLE_SPOTLESS}"
    const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.KOTLIN_GRADLE_DOKKA}"
    const val kotlinBinaryValidator =
        "org.jetbrains.kotlinx:binary-compatibility-validator:${Versions.KOTLIN_BINARY_VALIDATOR}"
    const val wirePlugin = "com.squareup.wire:wire-gradle-plugin:${Versions.WIRE}"
    const val coroutines =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KOTLIN_COROUTINE}"

    const val material = "com.google.android.material:material:${Versions.MATERIAL}"
    const val androidxAppcompat = "androidx.appcompat:appcompat:${Versions.ANDROIDX_APPCOMPAT}"
    const val androidxCore = "androidx.core:core-ktx:${Versions.ANDROIDX_CORE}"
    const val androidxLifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.ANDROIDX_LIFECYCLE}"
    const val androidxLifecycleProcess = "androidx.lifecycle:lifecycle-process:${Versions.ANDROIDX_LIFECYCLE}"

    // Compose
    const val activityCompose = "androidx.activity:activity-compose:${Versions.ACTIVITY_COMPOSE}"
    const val composeCompiler = "androidx.compose.compiler:compiler:${Versions.COMPOSE}"
    const val composeUi = "androidx.compose.ui:ui:${Versions.COMPOSE}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${Versions.COMPOSE}"
    const val composeFoundation = "androidx.compose.foundation:foundation:${Versions.COMPOSE}"
    const val composeMaterial = "androidx.compose.material:material:${Versions.COMPOSE}"

    const val wireRuntime = "com.squareup.wire:wire-runtime:${Versions.WIRE}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.RETROFIT}"
    const val retrofitWireConverter = "com.squareup.retrofit2:converter-wire:${Versions.RETROFIT}"
    const val okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.OKHTTP}"

    const val liveKit = "io.livekit:livekit-android:${Versions.LIVE_KIT}"
}
