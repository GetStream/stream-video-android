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
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import io.getstream.video.android.Configuration

plugins {
    alias(libs.plugins.maven.publish)
    id("io.getstream.video.android.library")
    id("io.getstream.spotless")
}

android {
    namespace = "io.getstream.video.android.mock"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    resourcePrefix = "stream_video_"
}

baselineProfile {
    baselineProfileOutputDir = "."
    filter {
        include("io.getstream.video.android.model.**")
    }
}

dependencies {
    api(project(":stream-video-android-core"))
}

mavenPublishing {
    coordinates(
        groupId = Configuration.artifactGroup,
        artifactId = "stream-video-android-previewdata",
        version = rootProject.version.toString(),
    )
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        ),
    )
}
