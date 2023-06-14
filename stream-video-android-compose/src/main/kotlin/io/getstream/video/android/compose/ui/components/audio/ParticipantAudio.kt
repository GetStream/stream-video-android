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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockParticipant

@Composable
public fun ParticipantAudio(
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    style: AudioRendererStyle = RegularAudioRendererStyle(),
    microphoneIndicatorContent: @Composable BoxScope.(ParticipantState) -> Unit = {
    }
) {
    val user by participant.user.collectAsStateWithLifecycle()
    val nameOrId by participant.userNameOrId.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(modifier = Modifier.size(65.dp)) {
            UserAvatar(
                user = user,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            )

            if (style.isSpeaking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(style.speakingBorder, CircleShape)

                )
            } else if (style.isShowingMicrophoneAvailability) {
                microphoneIndicatorContent.invoke(this, participant)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = nameOrId,
            fontSize = 12.sp,
            color = VideoTheme.colors.textHighEmphasis,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            if (style.isShowingRoleBadge) {
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = user.role,
                fontSize = 9.sp,
                color = VideoTheme.colors.textLowEmphasis,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantAudioPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantAudio(
            modifier = Modifier.size(100.dp),
            participant = mockParticipant,
            style = RegularAudioRendererStyle(isSpeaking = true)
        )
    }
}
