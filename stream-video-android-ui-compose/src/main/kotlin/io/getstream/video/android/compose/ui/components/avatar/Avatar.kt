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

package io.getstream.video.android.compose.ui.components.avatar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

/**
 * Loads the [imageUrl] into a clipped image with a subtle border, and falls back to an
 * [InitialsAvatar] built from [fallbackText] when there is no image or loading fails.
 *
 * In inspection mode the image is replaced with [previewModePlaceholder].
 */
@Composable
internal fun Avatar(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    fallbackText: String? = null,
    shape: Shape = CircleShape,
    imageScale: ContentScale = ContentScale.Crop,
    imageDescription: String? = null,
    imageRequestSize: IntSize = IntSize(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE),
    @DrawableRes loadingPlaceholder: Int? = LocalAvatarPreviewProvider.getLocalAvatarLoadingPlaceholder(),
    @DrawableRes previewModePlaceholder: Int = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
    textStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
) {
    if (LocalInspectionMode.current && !imageUrl.isNullOrEmpty()) {
        Image(
            modifier = modifier
                .fillMaxSize()
                .avatarBorder(shape)
                .clip(shape)
                .testTag("avatar"),
            painter = painterResource(id = previewModePlaceholder),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        return
    }

    if (imageUrl.isNullOrEmpty() && !fallbackText.isNullOrBlank()) {
        InitialsAvatar(
            modifier = modifier,
            text = fallbackText,
            textStyle = textStyle,
            shape = shape,
        )
        return
    }

    val clickableModifier: Modifier = if (onClick != null) {
        modifier.clickable(
            onClick = onClick,
            indication = ripple(bounded = false),
            interactionSource = remember { MutableInteractionSource() },
        )
    } else {
        modifier
    }

    CoilImage(
        modifier = clickableModifier.avatarBorder(shape).clip(shape),
        imageModel = { imageUrl },
        imageOptions = ImageOptions(
            contentDescription = imageDescription,
            contentScale = imageScale,
            requestSize = imageRequestSize,
        ),
        previewPlaceholder = painterResource(id = previewModePlaceholder),
        component = rememberImageComponent {
            +CrossfadePlugin()
            loadingPlaceholder?.let {
                +PlaceholderPlugin.Loading(painterResource(id = it))
            }
        },
        failure = {
            InitialsAvatar(
                modifier = modifier,
                text = fallbackText.orEmpty(),
                textStyle = textStyle,
                shape = shape,
            )
        },
    )
}

@Composable
private fun Modifier.avatarBorder(shape: Shape): Modifier =
    border(StreamTokens.strokeW100, VideoTheme.colors.borderCoreOpacitySubtle, shape)

internal const val DEFAULT_IMAGE_SIZE = -1
