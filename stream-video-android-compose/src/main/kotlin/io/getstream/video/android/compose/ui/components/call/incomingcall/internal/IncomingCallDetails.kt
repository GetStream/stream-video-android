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

package io.getstream.video.android.compose.ui.components.call.incomingcall.internal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.CallStatus

@Composable
internal fun IncomingCallDetails(
    modifier: Modifier = Modifier,
    participants: List<ParticipantState>
) {
    Column(modifier = modifier.fillMaxWidth()) {

        ParticipantAvatars(participants = participants)

        Spacer(modifier = Modifier.height(VideoTheme.dimens.callParticipantsAvatarsMargin))

        ParticipantInformation(
            callStatus = CallStatus.Incoming,
            participants = participants
        )
    }
}

@Preview
@Composable
private fun IncomingCallDetailsPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        IncomingCallDetails(
            participants = mockParticipantList
        )
    }
}
