import io.getstream.video.android.Configuration
import io.getstream.video.android.Dependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("binary-compatibility-validator")
}

android {
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":stream-video-android-compose"))

    implementation(platform("com.google.firebase:firebase-bom:30.3.1"))
    implementation("com.google.firebase:firebase-messaging")

    implementation(Dependencies.material)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}