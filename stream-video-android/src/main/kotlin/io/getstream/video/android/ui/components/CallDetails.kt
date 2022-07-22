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

package io.getstream.video.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant

@Composable
public fun CallDetails(
    room: Room,
    participants: List<Participant>,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier) {
        ParticipantsList(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            room = room,
            participants = participants
        )

        Row(modifier = Modifier.fillMaxWidth()) {
        }
    }
}
