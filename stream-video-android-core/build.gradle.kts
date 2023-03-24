import io.getstream.video.android.Configuration
import io.getstream.video.android.Dependencies

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("io.getstream.android.library")
    id("io.getstream.video.generateServices")
    id("io.getstream.spotless")
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.wire.get().pluginId)
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", Configuration.artifactGroup)
    set("PUBLISH_ARTIFACT_ID", "stream-video-android")
    set("PUBLISH_VERSION", rootProject.extra.get("rootVersionName"))
}

apply(from = "${rootDir}/scripts/publish-module.gradle")

wire {
    kotlin {
        rpcRole = "none"
    }

    protoPath {
        srcDir("src/main/proto")
    }
}

generateRPCServices {}

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            consumerProguardFiles("consumer-proguard-rules.pro")
        }

        debug {
            isMinifyEnabled = true
            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            consumerProguardFiles("consumer-proguard-rules.pro")
        }
    }

    resourcePrefix = "stream_video_"

    sourceSets.configureEach {
        kotlin.srcDir("build/generated/source/services")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    api(Dependencies.streamWebRTC)
    api(Dependencies.streamWebRTCUI)

    implementation(Dependencies.androidxCore)
    implementation(Dependencies.androidxAppcompat)
    implementation(Dependencies.material)

    // lifecycle
    implementation(Dependencies.androidxLifecycleRuntime)
    implementation(Dependencies.androidxLifecycleProcess)
    implementation(Dependencies.androidxLifecycleViewModel)

    // coroutines
    implementation(Dependencies.coroutines)

    // API & Protobuf
    api(Dependencies.wireRuntime)
    implementation(Dependencies.retrofit)
    implementation(Dependencies.retrofitWireConverter)
    implementation(Dependencies.okhttpLoggingInterceptor)
    implementation(Dependencies.kotlinSerializationJson)
    implementation(Dependencies.retrofitMoshi)
    implementation(Dependencies.retrofitScalars)

    implementation(Dependencies.moshi)
    implementation(Dependencies.moshiKotlin)
    implementation(Dependencies.moshiAdapters)

    // Stream
    implementation(Dependencies.streamLog)
    implementation(Dependencies.streamPush)
    implementation(Dependencies.streamPushDelegate)

    // Unit Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}