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

package io.getstream.video.android.compose.ui.components.participants.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamIconButton
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
            .background(VideoTheme.colors.baseSheetPrimary),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier
                .padding(
                    start = VideoTheme.dimens.componentPaddingStart,
                    end = VideoTheme.dimens.componentPaddingEnd,
                ),
            text = resources.getQuantityString(
                R.plurals.stream_video_call_participants_info_number_of_participants,
                numberOfParticipants,
                numberOfParticipants,
            ),
            style = VideoTheme.typography.titleS,
            color = VideoTheme.colors.basePrimary,
        )

        StreamIconButton(
            onClick = onBackPressed,
            icon = Icons.Default.Close,
            style = VideoTheme.styles.buttonStyles.onlyIconIconButtonStyle(),
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
