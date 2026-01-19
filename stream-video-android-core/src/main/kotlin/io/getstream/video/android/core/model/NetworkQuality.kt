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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import stream.video.sfu.models.ConnectionQuality

/**
 * Represents the network connection quality of a video call.
 */
@Stable
public sealed class NetworkQuality(public open val quality: Float) {

    @Stable
    public data class UnSpecified(
        public override val quality: Float = 0f,
    ) : NetworkQuality(quality)

    @Stable
    public data class Poor(
        public override val quality: Float = 0.33f,
    ) : NetworkQuality(quality)

    @Stable
    public data class Good(
        public override val quality: Float = 0.66f,
    ) : NetworkQuality(quality)

    @Stable
    public data class Excellent(
        public override val quality: Float = 1.0f,
    ) : NetworkQuality(quality)

    public companion object {

        public fun fromConnectionQuality(connectionQuality: ConnectionQuality): NetworkQuality {
            return when (connectionQuality) {
                ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED -> UnSpecified()
                ConnectionQuality.CONNECTION_QUALITY_POOR -> Poor()
                ConnectionQuality.CONNECTION_QUALITY_GOOD -> Good()
                ConnectionQuality.CONNECTION_QUALITY_EXCELLENT -> Excellent()
            }
        }
    }
}
