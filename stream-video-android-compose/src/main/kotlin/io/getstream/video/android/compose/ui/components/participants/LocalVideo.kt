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

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState

@Composable
public fun LocalVideo(
    call: Call,
    callParticipant: CallParticipantState,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit = {}
) {
    Surface(
        modifier = modifier.shadow(elevation = 8.dp),
        elevation = 8.dp
    ) {
        val videoTrack = callParticipant.track

        if (videoTrack != null) {
            VideoRenderer(
                modifier = Modifier.fillMaxSize(),
                call = call,
                videoTrack = videoTrack,
                onRender = {
                    it.elevation = 8f
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            val icons = VideoTheme.icons
            Icon(
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp),
                painter = if (callParticipant.hasAudio) icons.micOn else icons.micOff,
                contentDescription = "Audio enabled: ${callParticipant.hasAudio}",
                tint = Color.White
            )

            Icon(
                modifier = Modifier
                    .padding(4.dp)
                    .size(24.dp),
                painter = if (callParticipant.hasVideo) icons.videoCamOn else icons.videoCamOff,
                contentDescription = "Video enabled: ${callParticipant.hasVideo}",
                tint = Color.White,
            )
        }
    }
}
