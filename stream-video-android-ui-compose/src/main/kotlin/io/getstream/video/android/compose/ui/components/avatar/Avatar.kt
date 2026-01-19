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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

/**
 * An avatar that renders an image or a fallback text. In case the image URL
 * is empty or there was an error loading the image, it falls back to showing initials.
 * If needed, the initials font size is gradually decreased automatically until the text fits within the avatar boundaries.
 *
 * @param modifier Modifier used for styling.
 * @param imageUrl The URL of the image to be displayed.
 * @param fallbackText The fallback text to be used for the initials avatar.
 * @param shape The shape of the avatar.
 * @param imageScale The scale rule used for the image.
 * @param imageDescription Accessibility description for the image.
 * @param imageRequestSize The image size to be requested.
 * @param loadingPlaceholder Placeholder image to be displayed while loading the remote image.
 * @param previewModePlaceholder Placeholder image to be displayed in Compose previews (IDE).
 * @param textStyle The text style of the [fallbackText] text. The `fontSize`, `fontFamily` and `fontWeight` properties are used.
 * If the font size is too large, it will be gradually decreased automatically.
 * @param textOffset Offset to be applied to the initials text.
 * @param onClick Handler to be called when the user clicks on the avatar.
 */
@Composable
internal fun Avatar(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    fallbackText: String? = null,
    shape: Shape = VideoTheme.shapes.circle,
    imageScale: ContentScale = ContentScale.Crop,
    imageDescription: String? = null,
    imageRequestSize: IntSize = IntSize(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE),
    @DrawableRes loadingPlaceholder: Int? = LocalAvatarPreviewProvider.getLocalAvatarLoadingPlaceholder(),
    @DrawableRes previewModePlaceholder: Int = LocalAvatarPreviewProvider.getLocalAvatarPreviewPlaceholder(),
    textStyle: TextStyle = VideoTheme.typography.titleM,
    textOffset: DpOffset = DpOffset(0.dp, 0.dp),
    onClick: (() -> Unit)? = null,
) {
    if (LocalInspectionMode.current && !imageUrl.isNullOrEmpty()) {
        Image(
            modifier = modifier
                .fillMaxSize()
                .clip(CircleShape)
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
            textOffset = textOffset,
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
        modifier = clickableModifier.clip(shape),
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
                textOffset = textOffset,
                shape = shape,
            )
        },
    )
}

@Preview
@Composable
private fun AvatarInitialPreview() {
    VideoTheme {
        Avatar(
            modifier = Modifier.size(72.dp),
            fallbackText = "Thierry",
        )
    }
}

@Preview
@Composable
internal fun AvatarImagePreview() {
    VideoTheme {
        Avatar(
            modifier = Modifier.size(72.dp),
            fallbackText = null,
            previewModePlaceholder = R.drawable.stream_video_call_sample,
        )
    }
}

internal const val DEFAULT_IMAGE_SIZE = -1
