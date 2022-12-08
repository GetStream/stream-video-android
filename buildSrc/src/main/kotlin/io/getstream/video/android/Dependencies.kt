package io.getstream.video.android

object Versions {
    internal const val ANDROID_GRADLE_PLUGIN = "7.3.0"
    internal const val ANDROID_GRADLE_SPOTLESS = "6.7.0"
    internal const val GRADLE_NEXUS_PUBLISH_PLUGIN = "1.1.0"
    internal const val KOTLIN = "1.7.20"
    internal const val KOTLIN_GRADLE_DOKKA = "1.7.20"
    internal const val KOTLIN_SERIALIZATION_JSON = "1.4.0"
    internal const val KOTLIN_BINARY_VALIDATOR = "0.11.1"
    internal const val KOTLIN_COROUTINE = "1.6.4"

    internal const val MATERIAL = "1.6.1"
    internal const val ANDROIDX_APPCOMPAT = "1.4.2"
    internal const val ANDROIDX_CORE = "1.8.0"
    internal const val ANDROIDX_LIFECYCLE = "2.5.1"
    internal const val ACTIVITY_COMPOSE = "1.5.0"

    internal const val COMPOSE = "1.3.0"
    const val COMPOSE_COMPILER = "1.3.2"
    internal const val COIL = "2.2.1"

    internal const val WIRE = "4.4.1"
    internal const val RETROFIT = "2.9.0"
    internal const val OKHTTP = "4.10.0"
    internal const val MOSHI = "1.14.0"

    internal const val WEBRTC = "104.5112.05"
    internal const val STREAM = "6.0.0-beta1"
    internal const val STREAM_LOG = "1.1.3"
    internal const val STREAM_PUSH = "1.0.2"

    internal const val ANDROIDX_TEST = "1.4.0"
    internal const val BASE_PROFILE = "1.2.0"
    internal const val MACRO_BENCHMARK = "1.1.0"
    internal const val ANDROIDX_UI_AUTOMATOR = "2.2.0"
}

object Dependencies {
    const val androidGradlePlugin =
        "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}"
    const val gradleNexusPublishPlugin =
        "io.github.gradle-nexus:publish-plugin:${Versions.GRADLE_NEXUS_PUBLISH_PLUGIN}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val kotlinSerializationPlugin =
        "org.jetbrains.kotlin:kotlin-serialization:${Versions.KOTLIN}"
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
    const val androidxLifecycleViewModel =
        "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.ANDROIDX_LIFECYCLE}"
    const val androidxLifecycleRuntime =
        "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.ANDROIDX_LIFECYCLE}"
    const val androidxLifecycleProcess =
        "androidx.lifecycle:lifecycle-process:${Versions.ANDROIDX_LIFECYCLE}"

    // Compose
    const val activityCompose = "androidx.activity:activity-compose:${Versions.ACTIVITY_COMPOSE}"
    const val composeRuntime = "androidx.compose.runtime:runtime:${Versions.COMPOSE}"
    const val composeUi = "androidx.compose.ui:ui:${Versions.COMPOSE}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${Versions.COMPOSE}"
    const val composeFoundation = "androidx.compose.foundation:foundation:${Versions.COMPOSE}"
    const val composeMaterial = "androidx.compose.material:material:${Versions.COMPOSE}"
    const val composeIconsExtended =
        "androidx.compose.material:material-icons-extended:${Versions.COMPOSE}"
    const val composeCoil = "io.coil-kt:coil-compose:${Versions.COIL}"

    const val wireRuntime = "com.squareup.wire:wire-runtime:${Versions.WIRE}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.RETROFIT}"
    const val retrofitWireConverter = "com.squareup.retrofit2:converter-wire:${Versions.RETROFIT}"
    const val okhttpLoggingInterceptor =
        "com.squareup.okhttp3:logging-interceptor:${Versions.OKHTTP}"
    const val moshi = "com.squareup.moshi:moshi:${Versions.MOSHI}"
    const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:${Versions.MOSHI}"
    const val moshiAdapters = "com.squareup.moshi:moshi-adapters:${Versions.MOSHI}"

    const val webRTC = "com.github.webrtc-sdk:android:${Versions.WEBRTC}"

    const val streamLog = "io.getstream:stream-log:${Versions.STREAM_LOG}"
    const val streamLogAndroid = "io.getstream:stream-log-android:${Versions.STREAM_LOG}"
    const val streamPush = "io.getstream:stream-android-push:${Versions.STREAM_PUSH}"
    const val streamPushDelegate =
        "io.getstream:stream-android-push-delegate:${Versions.STREAM_PUSH}"
    const val streamPushFirebase =
        "io.getstream:stream-android-push-firebase:${Versions.STREAM_PUSH}"

    const val streamChatCompose = "io.getstream:stream-chat-android-compose:${Versions.STREAM}"
    const val streamChatOffline = "io.getstream:stream-chat-android-offline:${Versions.STREAM}"
    const val streamChatState = "io.getstream:stream-chat-android-state:${Versions.STREAM}"
    const val streamChatUiUtils = "io.getstream:stream-chat-android-ui-utils:${Versions.STREAM}"

    const val baseProfile =
        "androidx.profileinstaller:profileinstaller:${Versions.BASE_PROFILE}"
    const val macroBenchmark =
        "androidx.benchmark:benchmark-macro-junit4:${Versions.MACRO_BENCHMARK}"
    const val uiAutomator =
        "androidx.test.uiautomator:uiautomator:${Versions.ANDROIDX_UI_AUTOMATOR}"
    const val testRunner = "androidx.test:runner:${Versions.ANDROIDX_TEST}"
}
