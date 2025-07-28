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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.ui.components.video.DefaultMediaTrackFallbackContent
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.util.StreamVideoExperimentalApi
import io.getstream.video.android.ui.common.util.StreamVideoUiDelicateApi

private val defaultScalingType = VideoScalingType.SCALE_ASPECT_FILL
private val defaultVideoContainerModifier: BoxScope.() -> Modifier = {
    Modifier
        .fillMaxSize()
        .align(Alignment.Center)
}
private val defaultVideoComponentModifier: BoxScope.() -> Modifier =
    {
        Modifier
            .wrapContentSize()
            .align(Alignment.Center)
    }

private val defaultUpdateVisibility = true

/**
 * The [VideoRenderer] consists of two components one [Box] that acts as a container and another [AndroidView] that holds the actual [TextureView] for rendering the video.
 * Only modify these modifiers if you want to change the default behavior of the [VideoRenderer] and understand exactly the effect they have on the layout.
 */
@StreamVideoUiDelicateApi
@Immutable
public data class VideoRendererModifiersConfig(
    val containerModifier: BoxScope.() -> Modifier = defaultVideoContainerModifier,
    val componentModifier: BoxScope.() -> Modifier = defaultVideoComponentModifier,
)

/**
 * A scope to create a modifier config.
 */
@StreamVideoUiDelicateApi
@StreamVideoExperimentalApi(
    "Experimental exposure of internal modifiers for the video renderer. Maybe removed in the future without notice.",
)
@Immutable
public data class VideoRendererModifierScope(
    var containerModifier: BoxScope.() -> Modifier = defaultVideoContainerModifier,
    var componentModifier: BoxScope.() -> Modifier = defaultVideoComponentModifier,
)

/**
 * Builders scope for the builder function for the internal component modifiers.
 */
@StreamVideoExperimentalApi(
    "Experimental exposure of internal modifiers for the video renderer. Maybe removed in the future without notice.",
)
@StreamVideoUiDelicateApi
public inline fun videoComponentModifiers(
    block: VideoRendererModifierScope.() -> Unit,
): VideoRendererModifiersConfig {
    val scope = VideoRendererModifierScope()
    scope.block()
    return VideoRendererModifiersConfig(
        containerModifier = scope.containerModifier,
        componentModifier = scope.componentModifier,
    )
}

@OptIn(StreamVideoUiDelicateApi::class)
@Immutable
public data class VideoRendererConfig(
    val mirrorStream: Boolean = false,
    val modifiers: VideoRendererModifiersConfig = VideoRendererModifiersConfig(),
    val scalingType: VideoScalingType = defaultScalingType,
    val updateVisibility: Boolean = defaultUpdateVisibility,
    val fallbackContent: @Composable (Call) -> Unit = {},
    val badNetworkContent: @Composable (Call) -> Unit = {},
)

@OptIn(StreamVideoUiDelicateApi::class)
@Immutable
public data class VideoRendererConfigCreationScope(
    public var mirrorStream: Boolean = false,
    public var modifiers: VideoRendererModifiersConfig = VideoRendererModifiersConfig(),
    public var videoScalingType: VideoScalingType = defaultScalingType,
    public var updateVisibility: Boolean = defaultUpdateVisibility,
    public var fallbackContent: @Composable (Call) -> Unit = {
        DefaultMediaTrackFallbackContent(
            modifier = Modifier,
            call = it,
        )
    },
    public var badNetworkContent: @Composable (Call) -> Unit = {},
)

/**
 * A builder method for a video renderer config.
 */
@OptIn(StreamVideoUiDelicateApi::class)
public inline fun videoRenderConfig(
    block: VideoRendererConfigCreationScope.() -> Unit = {},
): VideoRendererConfig {
    val scope = VideoRendererConfigCreationScope()
    scope.block()
    return VideoRendererConfig(
        mirrorStream = scope.mirrorStream,
        modifiers = scope.modifiers,
        scalingType = scope.videoScalingType,
        updateVisibility = scope.updateVisibility,
        fallbackContent = scope.fallbackContent,
        badNetworkContent = scope.badNetworkContent,
    )
}
