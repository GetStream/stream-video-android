/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.getstream.video.android.Configuration
import java.io.FileInputStream
import java.util.*

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
    set("PUBLISH_ARTIFACT_ID", "stream-video-android-core")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    val envProps: File = rootProject.file(".env.properties")
    if (envProps.exists()) {
        val properties = Properties()
        properties.load(FileInputStream(envProps))
        buildTypes.forEach { buildType ->
            properties
                .filterKeys { "$it".startsWith("CORE") }
                .forEach {
                    buildType.buildConfigField("String", "${it.key}", "\"${it.value}\"")
                }
        }
    }

    resourcePrefix = "stream_video_"

    sourceSets.configureEach {
        kotlin.srcDir("build/generated/source/services")
    }

    packagingOptions {
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/LICENSE-notice.md")
    }
}

dependencies {
    // stream modules
    api(project(":stream-video-android-model"))
    implementation(project(":stream-video-android-datastore"))

    // webrtc
    api("io.github.webrtc-sdk:android:104.5112.10")

    // androidx
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // API & Protobuf
    api(libs.wire.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.scalars)
    implementation(libs.retrofit.wire.converter)
    implementation(libs.okhttp.logging)

    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    //implementation(libs.desugar)

    api(libs.threentenabp2)

    // Stream
    api(libs.stream.result)
    api(libs.stream.log.android)
    implementation(libs.stream.push)
    implementation(libs.stream.push.delegate)
    api(libs.stream.push.permissions)

    // unit tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.debug)

    // instrument tests
    androidTestImplementation(libs.stream.log.android)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation("androidx.test:rules:1.5.0")
}