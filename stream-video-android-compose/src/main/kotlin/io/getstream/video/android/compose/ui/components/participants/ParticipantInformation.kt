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

package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.CallStatus
import io.getstream.video.android.model.VideoParticipant

@Composable
public fun ParticipantInformation(
    callStatus: CallStatus,
    participants: List<VideoParticipant>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            text = text,
            fontSize = fontSize,
            style = VideoTheme.typography.title3,
            textAlign = TextAlign.Center,
            color = VideoTheme.colors.textHighEmphasis
        )

        Text(
            text = when (callStatus) {
                CallStatus.INCOMING -> "Incoming call..."
                CallStatus.OUTGOING -> "Calling..."
                CallStatus.CALLING -> "0:33" // TODO - observe current calling time
            },
            style = VideoTheme.typography.body,
            color = VideoTheme.colors.textLowEmphasis
        )
    }
}

// TODO - localize all this
private fun buildSmallCallText(participants: List<VideoParticipant>): String {
    val names = participants.map { it.user!!.name }

    return if (names.size == 1) {
        names.first()
    } else {
        "${names[0]} and ${names[1]}"
    }
}

private fun buildLargeCallText(participants: List<VideoParticipant>): String {
    val initial = buildSmallCallText(participants)

    return "$initial and +${participants.size - 2} more"
}
