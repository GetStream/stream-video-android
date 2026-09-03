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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.core.utils.initials

/**
 * An avatar showing the initials of [text] on the avatar background color.
 *
 * @param modifier The modifier applied to the avatar. The avatar is square, so one dimension is enough.
 * @param text The text the initials are taken from, usually the user name.
 * @param textStyle The typography of the initials, or null to pick one from the avatar size.
 * @param shape The shape the avatar is clipped to.
 * @param initialsTransformer Maps [text] to the initials that are drawn.
 */
@Composable
internal fun InitialsAvatar(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle? = null,
    shape: Shape = CircleShape,
    initialsTransformer: (String) -> String = { it.initials() },
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(color = VideoTheme.colors.avatarBgDefault),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsTransformer(text),
            style = textStyle ?: maxWidth.toAvatarTextStyle(),
            color = VideoTheme.colors.avatarTextDefault,
            maxLines = 1,
        )
    }
}

/**
 * Picks the initials typography for an avatar of this size.
 */
@Composable
@ReadOnlyComposable
internal fun Dp.toAvatarTextStyle(): TextStyle {
    val typography = VideoTheme.typography
    return when {
        this < StreamTokens.size24 -> typography.metadataEmphasis
        this < StreamTokens.size32 -> typography.captionEmphasis
        this < StreamTokens.size48 -> typography.bodyEmphasis
        this < StreamTokens.size80 -> typography.headingLarge
        else -> typography.numericExtraLarge
    }
}
