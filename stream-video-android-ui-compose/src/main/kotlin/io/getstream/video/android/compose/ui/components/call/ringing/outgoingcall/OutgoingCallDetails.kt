/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewMemberListState

/**
 * A details of an outgoing call that displays a list of participant information.
 *
 * @param isVideoType The type of call, Audio or Video.
 * @param participants A list of participants to be displayed.
 */
@Composable
public fun OutgoingCallDetails(
    modifier: Modifier = Modifier,
    isVideoType: Boolean = true,
    participants: List<MemberState>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!isVideoType) {
            ParticipantAvatars(members = participants)

            Spacer(modifier = Modifier.height(32.dp))
        }

        ParticipantInformation(
            isVideoType = isVideoType,
            callStatus = CallStatus.Outgoing,
            members = participants,
        )
    }
}

@Preview
@Composable
private fun OutgoingCallDetailsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        OutgoingCallDetails(
            isVideoType = false,
            participants = previewMemberListState,
        )
    }
}
