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

package io.getstream.video.android.core.model

import io.getstream.video.android.core.Call
import stream.video.sfu.models.PublishOption

/**
 * Options to configure video publishing.
 * @param codec The preferred video codec to use for publishing.
 * @param maxBitrate The preferred maximum bitrate to use for publishing.
 * @param maxSimulcastLayers The maximum number of simulcast layers to use for publishing.
 *
 * @see VideoCodec
 * @see [Call.updatePreferredPublishOptions]
 */
data class PreferredVideoPublishOptions(
    val codec: VideoCodec? = null,
    val maxBitrate: Int? = null,
    val maxSimulcastLayers: Int? = null,
)

/**
 * Contains the video codecs supported by the SDK.
 *
 * @see [Call.updatePreferredPublishOptions]
 */
enum class VideoCodec {
    H264,
    VP8,
    VP9,
    AV1,
    ;

    /**
     * Checks if the codec supports SVC (Scalable Video Coding).
     * Scalable Video Coding allows encoding multiple video layers (e.g., resolution or quality) into a single stream.
     *
     * @return True if the codec supports SVC, false otherwise.
     */
    fun supportsSvc() = this == VP9 || this == AV1
}

fun PublishOption.getScalabilityMode(): String =
    "L${max_spatial_layers}T${max_temporal_layers}" + if (max_spatial_layers > 1) "_KEY" else ""
