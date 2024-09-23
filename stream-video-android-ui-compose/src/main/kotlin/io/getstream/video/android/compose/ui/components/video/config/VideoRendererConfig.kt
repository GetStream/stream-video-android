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

package io.getstream.video.android.compose.ui.components.video.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.ui.components.video.DefaultMediaTrackFallbackContent
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.Call

@Immutable
public data class VideoRendererConfig(
    val mirrorStream: Boolean = false,
    val scalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    val fallbackContent: @Composable (Call) -> Unit = {},
)

@Immutable
public data class VideoRendererConfigCreationScope(
    public var mirrorStream: Boolean = false,
    public var videoScalingType: VideoScalingType = VideoScalingType.SCALE_ASPECT_FILL,
    public var fallbackContent: @Composable (Call) -> Unit = {
        DefaultMediaTrackFallbackContent(
            modifier = Modifier,
            call = it,
        )
    },
)

/**
 * A builder method for a video renderer config.
 */
public inline fun videoRenderConfig(
    block: VideoRendererConfigCreationScope.() -> Unit = {},
): VideoRendererConfig {
    val scope = VideoRendererConfigCreationScope()
    scope.block()
    return VideoRendererConfig(
        mirrorStream = scope.mirrorStream,
        scalingType = scope.videoScalingType,
        fallbackContent = scope.fallbackContent,
    )
}
