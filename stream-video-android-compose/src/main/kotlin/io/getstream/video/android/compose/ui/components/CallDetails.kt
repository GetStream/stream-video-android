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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Flip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.model.Room

@Composable
public fun CallDetails(
    room: Room,
    onEndCall: () -> Unit,
    onCameraToggled: (Boolean) -> Unit,
    onMicrophoneToggled: (Boolean) -> Unit,
    onCameraFlipped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val participant by room.localParticipant.collectAsState(initial = null)
    val isMicrophoneEnabled = participant?.hasAudio ?: false
    val isCameraEnabled = participant?.hasVideo ?: false

    val cardShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    Column(
        modifier = modifier.clip(cardShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                modifier = Modifier
                    .background(
                        color = if (!isMicrophoneEnabled) Color.Red else Color.Black,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clickable { onMicrophoneToggled(!isMicrophoneEnabled) },
                tint = Color.White,
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                modifier = Modifier
                    .background(
                        color = Color.Red,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clickable(onClick = onEndCall),
                tint = Color.White,
                imageVector = Icons.Default.CallEnd,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                modifier = Modifier
                    .background(
                        color = if (!isCameraEnabled) Color.Red else Color.Black,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clickable { onCameraToggled(!isCameraEnabled) },
                tint = Color.White,
                imageVector = Icons.Default.Camera,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(16.dp))

            Icon(
                modifier = Modifier
                    .background(
                        color = Color.Black,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clickable { onCameraFlipped() },
                tint = Color.White,
                imageVector = Icons.Default.Flip,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}
