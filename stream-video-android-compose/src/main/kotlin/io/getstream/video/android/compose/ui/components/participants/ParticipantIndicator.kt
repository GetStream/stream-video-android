/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

@Composable
public fun ParticipantIndicatorIcon(
    number: Int,
    numberColor: Color = Color.White,
    numberBackgroundColor: Color = VideoTheme.colors.textLowEmphasis,
    showNumber: Boolean = true,
    onClick: () -> Unit
) {
    Box {
        IconButton(
            onClick = { onClick.invoke() },
            modifier = Modifier.padding(
                start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.stream_video_ic_participants),
                contentDescription = stringResource(id = R.string.stream_video_call_participants_menu_content_description),
                tint = VideoTheme.colors.callDescription,
            )
        }

        if (showNumber) {
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .drawBehind {
                        drawRoundRect(
                            color = numberBackgroundColor,
                            cornerRadius = CornerRadius(this.size.maxDimension)
                        )
                    }
                    .padding(3.dp),
                text = number.toString(),
                fontSize = 12.sp,
                color = numberColor,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantIndicatorIconPreview() {
    VideoTheme {
        ParticipantIndicatorIcon(number = 10) {}
    }
}
