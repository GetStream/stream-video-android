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

import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.utils.initialsColors
import io.getstream.video.android.core.utils.initials

/**
 * Represents a special avatar case when we need to show the initials instead of an image. Usually happens when there
 * are no images to show in the avatar.
 *
 * @param text The initials to show.
 * @param modifier Modifier for styling.
 * @param shape The shape of the avatar.
 * @param textStyle The [TextStyle] that will be used for the initials.
 * @param textOffset The initials offset to apply to the avatar.
 * @param initialsTransformer A custom transformer to tweak initials.
 */
@Composable
internal fun InitialsAvatar(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = VideoTheme.typography.titleM,
    textOffset: DpOffset = DpOffset(0.dp, 0.dp),
    shape: Shape = VideoTheme.shapes.circle,
    initialsTransformer: (String) -> String = { it.initials() },
) {
    val colors = initialsColors(text = text)
    var fontSize by remember { mutableStateOf(textStyle.fontSize) }
    var readyToDrawText by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .widthIn(VideoTheme.dimens.genericS, VideoTheme.dimens.genericMax)
            .aspectRatio(1f)
            .clip(shape)
            .background(color = colors.second),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(textOffset.x, textOffset.y)
                .drawWithContent { if (readyToDrawText) drawContent() },
            text = initialsTransformer.invoke(text),
            fontSize = fontSize,
            style = textStyle.copy(color = colors.first),
            onTextLayout = { layoutResult ->
                Log.d("InitialsAvatar", "fontSize: $fontSize")
                if (layoutResult.didOverflowWidth || layoutResult.didOverflowHeight) {
                    fontSize *= 0.37
                } else {
                    readyToDrawText = true
                }
            },
        )
    }
}

@Preview
@Composable
private fun InitialsAvatarPreview() {
    VideoTheme {
        Column {
            Avatar(
                fallbackText = "Jaewoong Eum",
            )
            Spacer(modifier = Modifier.size(24.dp))
            Avatar(
                fallbackText = "Aleksandar Apostolov",
            )
            Spacer(modifier = Modifier.size(24.dp))
            Avatar(
                fallbackText = "Danie",
            )
            Spacer(modifier = Modifier.size(24.dp))
            Avatar(
                fallbackText = "Jaewoong Eum",
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
