/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayerOverlay
import io.getstream.video.android.core.Call
import io.getstream.video.android.hls.HlsVideoRenderer

/**
 * The viewer screen for a call the backend serves as HLS: the broadcast under the same overlay as
 * the WebRTC livestream player (live badge, participant count, duration and a mute toggle), plus a
 * leave button.
 */
@Composable
fun DemoHlsViewerScreen(
    call: Call,
    onLeave: () -> Unit,
) {
    var audioEnabled by rememberSaveable { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize()) {
        HlsVideoRenderer(
            call = call,
            modifier = Modifier.fillMaxSize(),
            audioEnabled = audioEnabled,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            LivestreamPlayerOverlay(
                call = call,
                isAudioEnabled = audioEnabled,
                onAudioToggle = { audioEnabled = it },
            )
            LeaveCallAction(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(45.dp),
            ) { onLeave() }
        }
    }
}
