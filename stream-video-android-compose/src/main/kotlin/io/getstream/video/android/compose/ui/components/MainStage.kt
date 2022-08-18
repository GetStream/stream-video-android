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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.getstream.video.android.model.VideoParticipant
import io.getstream.video.android.model.VideoRoom
import io.livekit.android.compose.VideoRenderer
import io.livekit.android.room.track.VideoTrack

@Composable
public fun MainStage(
    room: VideoRoom,
    speaker: VideoParticipant?,
    modifier: Modifier = Modifier
) {
    val track =
        speaker?.value?.videoTracks?.firstOrNull { it.second is VideoTrack }?.second as? VideoTrack

    if (track != null) {
        VideoRenderer(
            modifier = modifier,
            room = room.value,
            videoTrack = track
        )
    } else {
        Box(
            modifier = modifier
        ) {
            Image(
                modifier = Modifier.align(Alignment.Center),
                imageVector = Icons.Default.Call,
                contentDescription = null
            )
        }
    }
}
