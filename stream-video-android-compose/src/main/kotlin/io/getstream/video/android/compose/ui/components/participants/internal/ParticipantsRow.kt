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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockParticipants
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState

/**
 * Shows a row of call participants.
 *
 * @param call The state of the call.
 * @param participants List of participants to show.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun ParticipantsRow(
    call: Call?,
    participants: List<ParticipantState>,
    primarySpeaker: ParticipantState?,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(horizontal = VideoTheme.dimens.screenShareParticipantsRowPadding),
        horizontalArrangement = Arrangement.spacedBy(VideoTheme.dimens.screenShareParticipantsListItemMargin),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            items(items = participants, key = { it.user.value.id }) { participant ->
                ParticipantListItem(
                    call = call, participant = participant, primarySpeaker = primarySpeaker
                )
            }
        }
    )
}

/**
 * Shows a single call participant in a list.
 *
 * @param call The call state.
 * @param participant The participant to render.
 */
@Composable
private fun ParticipantListItem(
    call: Call?,
    participant: ParticipantState,
    primarySpeaker: ParticipantState?,
) {
    CallParticipant(
        modifier = Modifier
            .size(VideoTheme.dimens.screenShareParticipantItemSize)
            .clip(RoundedCornerShape(VideoTheme.dimens.screenShareParticipantsRadius)),
        call = call,
        participant = participant,
        labelPosition = Alignment.BottomStart,
        isScreenSharing = true,
        isFocused = participant.initialUser.id == primarySpeaker?.initialUser?.id,
        isShowConnectionQualityIndicator = false
    )
}

@Preview
@Composable
private fun ParticipantsRowPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantsRow(
            call = null, participants = mockParticipants, primarySpeaker = mockParticipants[0]
        )
    }
}
