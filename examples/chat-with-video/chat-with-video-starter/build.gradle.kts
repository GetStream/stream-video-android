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
    id("io.getstream.android.application.compose")
    id("io.getstream.spotless")
}

android {
    namespace = "io.getstream.video.chat_with_video_starter"
    compileSdk = Configuration.compileSdk

    defaultConfig {
        applicationId = "io.getstream.video.chat_with_video_starter"
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

    val envProps: File = rootProject.file(".env.properties")
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

    // androidx
    implementation(libs.androidx.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)

    // compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.coil.compose)

    // Stream chat SDK & logger
    implementation(libs.stream.log.android)
    implementation(libs.stream.chat.compose)
    implementation(libs.stream.chat.offline)
    implementation(libs.stream.chat.state)
    implementation(libs.stream.chat.ui.utils)
}