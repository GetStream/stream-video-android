/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
@file:Suppress("UnstableApiUsage")

import io.getstream.video.android.Configuration

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("io.getstream.android.application.compose")

    id("com.google.gms.google-services")
}

android {
    //namespace = "io.getstream.video.android.tutorial.video"
    namespace = "io.getstream.android.samples.ringingcall"
    //namespace = "io.getstream.video.android"
    compileSdk = Configuration.compileSdk

    defaultConfig {
        //applicationId = "io.getstream.video.android.tutorial.video"
        applicationId = "io.getstream.android.samples.ringingcall"
        //applicationId = "io.getstream.video.android"
        minSdk = Configuration.minSdk
        targetSdk = Configuration.targetSdk
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":stream-video-android-ui-compose"))

    // Push Notification
    implementation(libs.stream.push)
    implementation(libs.stream.push.firebase)
    implementation(platform(libs.firebase.bom))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.lifecycle.runtime.compose)
}