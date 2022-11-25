import io.getstream.video.android.Configuration
import io.getstream.video.android.Dependencies
import io.getstream.video.android.Versions

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.getstream.video.chat_with_video_sample"
    compileSdk = Configuration.compileSdk

    defaultConfig {
        applicationId = "io.getstream.video.chat_with_video_sample"
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk
        versionCode = Configuration.versionCode
        versionName = Configuration.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.COMPOSE_COMPILER
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":stream-video-android"))
    implementation(project(":stream-video-android-compose"))
    implementation(project(":stream-video-android-pushprovider-firebase"))

    implementation(Dependencies.androidxCore)
    implementation(Dependencies.androidxAppcompat)
    implementation(Dependencies.material)
    implementation(Dependencies.androidxLifecycleRuntime)

    // Compose
    implementation(Dependencies.composeRuntime)
    implementation(Dependencies.composeUi)
    implementation(Dependencies.composeUiTooling)
    implementation(Dependencies.composeFoundation)
    implementation(Dependencies.composeMaterial)
    implementation(Dependencies.activityCompose)
    implementation(Dependencies.composeIconsExtended)
    implementation(Dependencies.composeCoil)

    // Stream Logger
    implementation(Dependencies.streamLogger)
    implementation(Dependencies.streamLoggerAndroid)
    implementation(Dependencies.streamChatCompose)
    implementation(Dependencies.streamChatOffline)
    implementation(Dependencies.streamChatState)
    implementation(Dependencies.streamChatUiUtils)
}