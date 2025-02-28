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

package io.getstream.video.android.compose.ui.components.call.ringing.incomingcall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewMemberListState

/**
 * Component that displays details for an incoming call.
 *
 * @param modifier Modifier for styling.
 * @param isVideoType The type of call, audio or video.
 * @param members A list of call members to be displayed.
 */
@Composable
public fun IncomingCallDetails(
    modifier: Modifier = Modifier,
    isVideoType: Boolean = true,
    members: List<MemberState>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ParticipantAvatars(members = members)

        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))

        ParticipantInformation(
            isVideoType = isVideoType,
            callStatus = CallStatus.Incoming,
            members = members,
        )
    }
}

@Preview
@Composable
private fun IncomingCallDetailsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        IncomingCallDetails(
            isVideoType = true,
            members = previewMemberListState,
        )
    }
}
