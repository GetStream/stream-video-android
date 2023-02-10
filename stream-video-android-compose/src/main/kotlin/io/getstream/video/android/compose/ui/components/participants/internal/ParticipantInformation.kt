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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.buildLargeCallText
import io.getstream.video.android.common.util.buildSmallCallText
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.model.CallUser

@Composable
internal fun ParticipantInformation(
    callStatus: CallStatus,
    participants: List<CallUser>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val text = if (participants.size < 3) {
            buildSmallCallText(participants)
        } else {
            buildLargeCallText(participants)
        }

        val fontSize = if (participants.size == 1) {
            VideoTheme.dimens.directCallUserNameTextSize
        } else {
            VideoTheme.dimens.groupCallUserNameTextSize
        }

        Text(
            modifier = Modifier.padding(horizontal = VideoTheme.dimens.participantsTextPadding),
            text = text,
            fontSize = fontSize,
            color = VideoTheme.colors.textHighEmphasis,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.alpha(VideoTheme.dimens.onCallStatusTextAlpha),
            text = when (callStatus) {
                CallStatus.Incoming -> "Incoming call..."
                CallStatus.Outgoing -> "Calling..."
                is CallStatus.Calling -> callStatus.duration
            },
            style = VideoTheme.typography.body,
            fontSize = VideoTheme.dimens.onCallStatusTextSize,
            fontWeight = FontWeight.Bold,
            color = VideoTheme.colors.textHighEmphasis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun ParticipantInformationPreview() {
    VideoTheme {
        ParticipantInformation(
            callStatus = CallStatus.Incoming,
            participants = mockParticipantList.map {
                CallUser(
                    id = it.id,
                    name = it.name,
                    role = it.role,
                    state = null,
                    imageUrl = it.profileImageURL ?: "",
                    createdAt = null,
                    updatedAt = null,
                    teams = emptyList()
                )
            }
        )
    }
}
