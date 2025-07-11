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
import io.getstream.video.android.tooling.util.StreamBuildFlavor.Development
import io.getstream.video.android.tooling.util.StreamBuildFlavor.E2eTesting
import io.getstream.video.android.tooling.util.StreamBuildFlavor.Production

/**
 * Defined flavors. Used in code logic.
 */

object StreamBuildFlavorUtil {
    val current: StreamBuildFlavor
        get() = StreamBuildFlavor.from(BuildConfig.FLAVOR)

    val isDevelopment: Boolean get() = current == Development
    val isProduction: Boolean get() = current == Production
    val isE2eTesting: Boolean get() = current == E2eTesting
}

private const val TYPE_DEVELOPMENT = "development"
private const val TYPE_PRODUCTION = "production"
private const val TYPE_E2E_TESTING = "e2etesting"

enum class StreamBuildFlavor(val type: String) {
    Development(TYPE_DEVELOPMENT),
    Production(TYPE_PRODUCTION),
    E2eTesting(TYPE_E2E_TESTING),
    ;

    companion object {
        fun from(type: String?): StreamBuildFlavor {
            return when (type) {
                null -> Development
                TYPE_DEVELOPMENT -> Development
                TYPE_PRODUCTION -> Production
                TYPE_E2E_TESTING -> E2eTesting
                else -> Development
            }
        }
    }
}
public object StreamEnvironments {
    const val demo = "demo"
    const val pronto = "pronto"
}
