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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import io.getstream.video.android.compose.utils.initialsGradient
import io.getstream.video.android.core.utils.initials

/**
 * Represents a special avatar case when we need to show the initials instead of an image. Usually happens when there
 * are no images to show in the avatar.
 *
 * @param initials The initials to show.
 * @param modifier Modifier for styling.
 * @param textStyle The [TextStyle] that will be used for the initials.
 * @param avatarOffset The initials offset to apply to the avatar.
 */
@Composable
internal fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    shape: Shape = VideoTheme.shapes.avatar,
    textStyle: TextStyle = VideoTheme.typography.title3Bold,
    avatarOffset: DpOffset = DpOffset(0.dp, 0.dp),
) {
    val initialsGradient = initialsGradient(initials = initials)

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = initialsGradient)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(avatarOffset.x, avatarOffset.y),
            text = initials.initials(),
            style = textStyle,
            color = VideoTheme.colors.avatarInitials
        )
    }
}

@Preview
@Composable
private fun InitialsAvatarPreview() {
    VideoTheme {
        Avatar(
            modifier = Modifier.size(56.dp),
            initials = "Jaewoong Eum"
        )
    }
}
