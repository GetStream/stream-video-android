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

package io.getstream.video.android.compose.ui.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.compose.utils.initialsColors
import io.getstream.video.android.core.utils.initials

/**
 * Represents a special avatar case when we need to show the initials instead of an image. Usually happens when there
 * are no images to show in the avatar.
 *
 * @param initials The initials to show.
 * @param modifier Modifier for styling.
 * @param shape The shape of the avatar.
 * @param textStyle The [TextStyle] that will be used for the initials.
 * @param avatarOffset The initials offset to apply to the avatar.
 * @param initialTransformer A custom transformer to tweak initials.
 */
@Composable
internal fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    shape: Shape = VideoTheme.shapes.circle,
    textSize: StyleSize = StyleSize.XL,
    textStyle: TextStyle = VideoTheme.typography.titleM,
    avatarOffset: DpOffset = DpOffset(0.dp, 0.dp),
    initialTransformer: (String) -> String = { it.initials() },
) {
    val colors = initialsColors(initials = initials)
    Box(
        modifier = modifier
            .widthIn(VideoTheme.dimens.genericS, VideoTheme.dimens.genericMax)
            .aspectRatio(1f)
            .clip(shape)
            .background(color = colors.second),
    ) {
        val resolvedTextSize = when (textSize) {
            StyleSize.L -> VideoTheme.dimens.textSizeL
            StyleSize.XS -> VideoTheme.dimens.textSizeXs
            StyleSize.S -> VideoTheme.dimens.textSizeS
            StyleSize.M -> VideoTheme.dimens.textSizeM
            StyleSize.XL -> VideoTheme.dimens.textSizeXl
            StyleSize.XXL -> VideoTheme.dimens.textSizeXxl
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(avatarOffset.x, avatarOffset.y),
            text = initialTransformer.invoke(initials),
            fontSize = resolvedTextSize,
            style = textStyle.copy(color = colors.first),
        )
    }
}

@Preview
@Composable
private fun InitialsAvatarPreview() {
    VideoTheme {
        Column {
            Avatar(
                initials = "Jaewoong Eum",
            )
            Spacer(modifier = Modifier.size(24.dp))
            Avatar(
                initials = "Aleksandar Apostolov",
            )
            Spacer(modifier = Modifier.size(24.dp))
            Avatar(
                initials = "Danie",
            )
            Spacer(modifier = Modifier.size(24.dp))
            Avatar(
                initials = "Jaewoong Eum",
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
