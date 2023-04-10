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

package io.getstream.video.android.compose.ui.components.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ShowCallInfo
import io.getstream.video.android.ui.common.R

@Composable
internal fun OverlayScreenSharingAppBar(
    sharingParticipant: ParticipantState,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = VideoTheme.colors.overlay),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier.padding(
                start = VideoTheme.dimens.callAppBarLeadingContentSpacingStart,
                end = VideoTheme.dimens.callAppBarLeadingContentSpacingEnd
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(id = R.string.back_button_content_description),
                tint = Color.White
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(
                id = R.string.stream_video_screen_sharing_title,
                sharingParticipant.user.collectAsState().value.name.ifEmpty { sharingParticipant.user }
            ),
            color = Color.White,
            style = VideoTheme.typography.title3Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(onClick = { onCallAction(ShowCallInfo) }) {
            Icon(
                modifier = Modifier
                    .background(
                        shape = CircleShape,
                        color = VideoTheme.colors.barsBackground
                    )
                    .padding(8.dp),
                painter = painterResource(id = R.drawable.ic_participants),
                contentDescription = stringResource(id = R.string.call_participants_menu_content_description),
                tint = VideoTheme.colors.textHighEmphasis
            )
        }
    }
}
