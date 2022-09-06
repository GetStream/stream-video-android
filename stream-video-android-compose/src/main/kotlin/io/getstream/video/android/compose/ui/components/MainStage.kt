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

package io.getstream.video.android.compose.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.getstream.video.android.compose.ui.components.participants.FloatingParticipantItem
import io.getstream.video.android.compose.ui.components.participants.ParticipantsContent
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.VideoRoom

@Composable
public fun MainStage(
    room: VideoRoom,
    localParticipant: CallParticipant,
    participants: List<CallParticipant>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ParticipantsContent(
            modifier = Modifier.fillMaxSize(),
            room = room,
            participants = participants,
            localParticipant = localParticipant
        )

        val localTrack = localParticipant.track

        if (localTrack != null) {
            FloatingParticipantItem(room, localParticipant)
        }
    }
}
