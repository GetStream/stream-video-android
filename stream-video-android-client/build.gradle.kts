plugins {
    id("io.getstream.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.getstream.video.android.client"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-default=all",
            "-Xexplicit-api=warning",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=io.getstream.video.android.core.internal.InternalStreamVideoApi",
        )
    }
}

dependencies {


    implementation("com.squareup:kotlinpoet:1.15.0")
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    implementation(project(":stream-annotations"))
    implementation(project(":stream-video-android-core"))

    api(libs.stream.webrtc)
    implementation(libs.audioswitch)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    api(libs.wire.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)
    implementation(libs.retrofit.wire.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.ituDate)
    api(libs.threentenabp2)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    api(libs.stream.log)
    implementation(libs.stream.push)
    implementation(libs.stream.push.delegate)
    api(libs.stream.push.permissions)
    implementation(libs.tink)

    // Annotation processors
    ksp(project(":stream-annotations"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}