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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.CallParticipant
import io.getstream.video.android.model.Room

@Composable
public fun FloatingParticipantItem(
    room: Room,
    callParticipant: CallParticipant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(height = 100.dp, width = 75.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
    ) {
        val videoTrack = callParticipant.track

        if (videoTrack != null) {
            VideoRenderer(
                modifier = Modifier.fillMaxSize(),
                room = room,
                videoTrack = videoTrack
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            val icons = VideoTheme.icons
            Icon(
                modifier = Modifier.padding(8.dp),
                painter = if (callParticipant.hasAudio) icons.micOn else icons.micOff,
                contentDescription = "Audio enabled: ${callParticipant.hasAudio}",
                tint = Color.White
            )

            Icon(
                modifier = Modifier.padding(8.dp),
                painter = if (callParticipant.hasVideo) icons.videoCam else icons.videoCamOff,
                contentDescription = "Video enabled: ${callParticipant.hasVideo}",
                tint = Color.White
            )
        }
    }
}
