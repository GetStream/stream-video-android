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

@file:OptIn(ExperimentalComposeUiApi::class)

package io.getstream.video.android.compose.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.getstream.video.android.compose.ui.components.base.styling.CompositeStyleProvider
import io.getstream.video.android.core.header.HeadersUtil
import io.getstream.video.android.core.header.VersionPrefixHeader

/**
 * Local providers for various properties we connect to our components, for styling.
 */
private val LocalColors = compositionLocalOf<StreamColors> {
    error("No colors provided! Make sure to wrap all usages of Stream components in a VideoTheme.")
}

private val LocalDimens = compositionLocalOf<StreamDimens> {
    error("No dimens provided! Make sure to wrap all usages of Stream components in a VideoTheme.")
}
private val LocalTypography = compositionLocalOf<StreamTypography> {
    error(
        "No typography provided! Make sure to wrap all usages of Stream components in a VideoTheme.",
    )
}
private val LocalShapes = compositionLocalOf<StreamShapes> {
    error("No shapes provided! Make sure to wrap all usages of Stream components in a VideoTheme.")
}
private val LocalStyles = compositionLocalOf<CompositeStyleProvider> {
    error(
        "No styles provided! Make sure to wrap all usages of Stream components in a VideoTheme.",
    )
}

/**
 * The local composition containing the current [VideoComponentFactory].
 */
public val LocalComponentFactory: ProvidableCompositionLocal<VideoComponentFactory> =
    compositionLocalOf {
        error(
            "No component factory provided! Make sure to wrap all usages of Stream components " +
                "in a VideoTheme.",
        )
    }

/**
 * The local composition containing the current [VideoUiConfig].
 */
public val LocalVideoUiConfig: ProvidableCompositionLocal<VideoUiConfig> =
    compositionLocalOf {
        error(
            "No VideoUiConfig provided! Make sure to wrap all usages of Stream components " +
                "in a VideoTheme.",
        )
    }

/**
 * Our theme that provides all the important properties for styling to the user.
 *
 * @param config Central behavioral configuration for the Video SDK. See [VideoUiConfig].
 * @param colors The set of colors we provide, wrapped in [StreamColors].
 * @param dimens The set of dimens we provide, wrapped in [StreamDimens].
 * @param typography The set of typography styles we provide, wrapped in [StreamTypography].
 * @param shapes The set of shapes we provide, wrapped in [StreamShapes].
 * @param componentFactory Provide to customize the components used throughout the UI.
 * @param content The content shown within the theme wrapper.
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
public fun VideoTheme(
    config: VideoUiConfig = VideoUiConfig(),
    colors: StreamColors = StreamColors.defaultColors(),
    dimens: StreamDimens = StreamDimens.defaultDimens(),
    typography: StreamTypography = StreamTypography.defaultTypography(colors, dimens),
    shapes: StreamShapes = StreamShapes.defaultShapes(dimens),
    styles: CompositeStyleProvider = CompositeStyleProvider(),
    componentFactory: VideoComponentFactory = DefaultVideoComponentFactory,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) {
        HeadersUtil.VERSION_PREFIX_HEADER = VersionPrefixHeader.Compose
    }
    CompositionLocalProvider(
        LocalVideoUiConfig provides config,
        LocalColors provides colors,
        LocalDimens provides dimens,
        LocalTypography provides typography,
        LocalShapes provides shapes,
        LocalRippleConfiguration provides StreamRippleConfiguration.default(),
        LocalStyles provides styles,
        LocalComponentFactory provides componentFactory,
    ) {
        Box(
            modifier = Modifier.semantics {
                testTagsAsResourceId = config.allowUIAutomationTest
            },
        ) {
            content()
        }
    }
}

public interface StreamTheme {
    /**
     * Retrieves the current [VideoUiConfig] at the call site's position in the hierarchy.
     */
    public val config: VideoUiConfig
        @Composable @ReadOnlyComposable
        get() = LocalVideoUiConfig.current

    /**
     * Retrieves the current [StreamColors] at the call site's position in the hierarchy.
     */
    public val colors: StreamColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current

    /**
     * Retrieves the current [StreamDimens] at the call site's position in the hierarchy.
     */
    public val dimens: StreamDimens
        @Composable @ReadOnlyComposable
        get() = LocalDimens.current

    /**
     * Retrieves the current [StreamTypography] at the call site's position in the hierarchy.
     */
    public val typography: StreamTypography
        @Composable @ReadOnlyComposable
        get() = LocalTypography.current

    /**
     * Retrieves the current [StreamShapes] at the call site's position in the hierarchy.
     */
    public val shapes: StreamShapes
        @Composable @ReadOnlyComposable
        get() = LocalShapes.current

    /**
     * Retrieves the current [CompositeStyleProvider] at the call site's position in the hierarchy.
     */
    public val styles: CompositeStyleProvider
        @Composable @ReadOnlyComposable
        get() = LocalStyles.current

    /**
     * Retrieves the current [VideoComponentFactory] at the call site's position in the hierarchy.
     */
    public val componentFactory: VideoComponentFactory
        @Composable @ReadOnlyComposable
        get() = LocalComponentFactory.current
}

/**
 * Contains ease-of-use accessors for different properties used to style and customize the app
 * look and feel.
 */
public object VideoTheme : StreamTheme
