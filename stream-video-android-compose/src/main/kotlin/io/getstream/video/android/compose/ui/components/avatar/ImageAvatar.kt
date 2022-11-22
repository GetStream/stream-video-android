/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Renders an image the [painter] provides. It allows for customization,
 * uses the 'avatar' shape from [VideoTheme.shapes] for the clipping and exposes an [onClick] action.
 *
 * @param painter The painter for the image.
 * @param modifier Modifier for styling.
 * @param shape The shape of the avatar.
 * @param contentScale The scale option used for the content.
 * @param contentDescription Description of the image.
 * @param onClick OnClick action, that can be nullable.
 */
@Composable
public fun ImageAvatar(
    painter: Painter,
    modifier: Modifier = Modifier,
    shape: Shape = VideoTheme.shapes.avatar,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier: Modifier = if (onClick != null) {
        modifier.clickable(
            onClick = onClick,
            indication = rememberRipple(bounded = false),
            interactionSource = remember { MutableInteractionSource() }
        )
    } else {
        modifier
    }

    Image(
        modifier = clickableModifier.clip(shape),
        contentScale = contentScale,
        painter = painter,
        contentDescription = contentDescription
    )
}
