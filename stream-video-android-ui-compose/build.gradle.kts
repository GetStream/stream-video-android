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
import io.getstream.video.android.Configuration

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("io.getstream.android.library.compose")
//    id("io.getstream.spotless")
}

rootProject.extra.apply {
    set("PUBLISH_GROUP_ID", Configuration.artifactGroup)
    set("PUBLISH_ARTIFACT_ID", "stream-video-android-ui-compose")
    set("PUBLISH_VERSION", rootProject.extra.get("rootVersionName"))
}

apply(from = "${rootDir}/scripts/publish-module.gradle")

android {
    namespace = "io.getstream.video.android.compose"
    compileSdk = Configuration.compileSdk

    defaultConfig {
        minSdk = Configuration.minSdk
    }
}

baselineProfile {
    baselineProfileOutputDir = "."
    filter {
        include("io.getstream.video.android.compose.**")
    }
}

dependencies {
    api(project(":stream-video-android-core"))
    api(project(":stream-video-android-ui-core"))

    // androidx
    implementation(libs.stream.log.android)

    // compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.accompanist.permission)

    // image loading
    implementation(libs.landscapist.coil)
    implementation(libs.landscapist.animation)
    implementation(libs.landscapist.placeholder)

    // render scripts
    compileOnly(files("libs/renderscript-toolkit.aar"))

    // telephoto
    implementation(libs.telephoto)

    // preview
    compileOnly(project(":stream-video-android-previewdata"))
    testImplementation(project(":stream-video-android-previewdata"))
}