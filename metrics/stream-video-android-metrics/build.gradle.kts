import io.getstream.video.android.Configuration

plugins {
    id("io.getstream.android.application")
}

android {
    namespace = "io.getstream.video.android.metrics"
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("debug")
        }
    }

    flavorDimensions += "sdk"

    productFlavors {
        create("stream-video-android-core-baseline") {
            dimension = "sdk"
        }
        create("stream-video-android-core-stream") {
            dimension = "sdk"
        }
        create("stream-video-android-ui-xml-baseline") {
            dimension = "sdk"
        }
        create("stream-video-android-ui-xml-stream") {
            dimension = "sdk"
        }
        create("stream-video-android-ui-compose-baseline") {
            dimension = "sdk"
        }
        create("stream-video-android-ui-compose-stream") {
            dimension = "sdk"
        }
    }
}

afterEvaluate {
    android.productFlavors.forEach { flavor ->
        val flavorName = flavor.name
        // For compose flavors, we apply the compose plugin,
        // set up build features and add common compose dependencies.
        if (flavorName.contains("compose")) {
            val composePlugin = libs.plugins.compose.compiler.get()
//            plugins.apply(composePlugin.pluginId) // Enable with Kotlin 2.0+
            android.buildFeatures.compose = true
            val configurationName = "${flavorName}Implementation"
            dependencies.add(configurationName, libs.androidx.compose.ui)
            dependencies.add(configurationName, libs.androidx.compose.ui.tooling)
            dependencies.add(configurationName, libs.androidx.compose.foundation)
            dependencies.add(configurationName, libs.androidx.activity.compose)
            dependencies.add(configurationName, libs.androidx.lifecycle.runtime.compose)
            dependencies.add(configurationName, libs.androidx.lifecycle.viewmodel.compose)
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.appcompat)

    "stream-video-android-core-streamImplementation"(project(":stream-video-android-core"))

    "stream-video-android-ui-xml-streamImplementation"(project(":stream-video-android-ui-xml"))

    "stream-video-android-ui-compose-streamImplementation"(project(":stream-video-android-ui-compose"))
}
