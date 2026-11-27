/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

plugins {
    id("io.getstream.video.android.library.compose")
}

android {
    namespace = "io.getstream.video.android.hls"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

baselineProfile {
    baselineProfileOutputDir = "."
    filter {
        include("io.getstream.video.android.hls.**")
    }
}

dependencies {
    api(project(":stream-video-android-core"))

    // ExoPlayer only; the renderer supplies the surface, so media3-ui is not needed.
    // Version matches stream-chat-android 7.13 so an app using both gets a single Media3.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.activity.compose)
}
