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

package io.getstream.video.android.compose.ui.components.participants.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.common.R

/**
 * Renders the AppBar at the top of the
 * [io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu].
 *
 * @param numberOfParticipants Shows how many participants there are in the active call.
 * @param onBackPressed Handler when the user taps on the back button.
 */
@Composable
internal fun CallParticipantListAppBar(
    numberOfParticipants: Int,
    onBackPressed: () -> Unit,
) {
    val resources = LocalContext.current.resources

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.participantInfoMenuAppBarHeight)
            .background(VideoTheme.colors.barsBackground)
            .padding(VideoTheme.dimens.callAppBarPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = VideoTheme.dimens.callAppBarCenterContentSpacingStart,
                    end = VideoTheme.dimens.callAppBarCenterContentSpacingEnd,
                ),
            text = resources.getQuantityString(
                R.plurals.stream_video_call_participants_info_number_of_participants,
                numberOfParticipants,
                numberOfParticipants,
            ),
            style = VideoTheme.typography.title3,
            color = VideoTheme.colors.textHighEmphasis,
        )

        IconButton(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true),
            onClick = onBackPressed,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.stream_video_ic_close),
                    contentDescription = stringResource(
                        id = R.string.stream_video_back_button_content_description,
                    ),
                    tint = VideoTheme.colors.textHighEmphasis,
                )
            },
        )
    }
}

@Preview
@Composable
private fun CallParticipantsInfoAppBarPreview() {
    VideoTheme {
        CallParticipantListAppBar(
            numberOfParticipants = 10,
            onBackPressed = {},
        )
    }
}
