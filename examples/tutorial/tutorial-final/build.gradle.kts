import io.getstream.video.android.Configuration
import io.getstream.video.android.Dependencies
import io.getstream.video.android.Versions
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("io.getstream.android.application.compose")
    id("io.getstream.spotless")
}

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        applicationId = "io.getstream.video.android.tutorial_final"
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk
        versionCode = Configuration.versionCode
        versionName = Configuration.versionName
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

    val envProps: java.io.File = rootProject.file(".env.properties")
    if (envProps.exists()) {
        val properties = Properties()
        properties.load(FileInputStream(envProps))
        buildTypes.forEach { buildType ->
            properties
                .filterKeys { "$it".startsWith("SAMPLE") }
                .forEach {
                    buildType.buildConfigField("String", "${it.key}", "\"${it.value}\"")
                }
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":stream-video-android-compose"))

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

    // Stream logger
    implementation(Dependencies.streamLogAndroid)
}