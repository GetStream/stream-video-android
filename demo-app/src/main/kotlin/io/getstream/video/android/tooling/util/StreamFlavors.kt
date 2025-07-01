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

package io.getstream.video.android.tooling.util

import io.getstream.video.android.BuildConfig

/**
 * Defined flavors. Used in code logic.
 */
internal object StreamFlavors {
    const val development = "development"
    const val production = "production"
    const val e2etesting = "e2etesting"
}

object StreamBuildFlavorsUtil {
    fun getCurrentBuildFlavor(): StreamBuildFlavors {
        if (isProduction()) {
            return StreamBuildFlavors.Production
        }
        if (isE2eTesting()) {
            return StreamBuildFlavors.E2eTesting
        }
        return StreamBuildFlavors.Development
    }

    fun isDevelopment() = BuildConfig.FLAVOR == StreamBuildFlavors.Development.type
    fun isProduction() = BuildConfig.FLAVOR == StreamBuildFlavors.Production.type
    fun isE2eTesting() = BuildConfig.FLAVOR == StreamBuildFlavors.E2eTesting.type
}

sealed class StreamBuildFlavors(val type: String) {
    data object Development : StreamBuildFlavors("development")
    data object Production : StreamBuildFlavors("production")
    data object E2eTesting : StreamBuildFlavors("e2etesting")
}

public object StreamEnvironments {
    const val demo = "demo"
    const val pronto = "pronto"
}
