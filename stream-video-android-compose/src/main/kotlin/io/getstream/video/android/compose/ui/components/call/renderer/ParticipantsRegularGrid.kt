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

package io.getstream.video.android.compose.ui.components.call.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.internal.OrientationVideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

/**
 * Renders the participants are joining in a call when there are no screen sharing sessions, based on the orientation.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer renders each individual participant.
 */
@Composable
public fun ParticipantsRegularGrid(
    modifier: Modifier = Modifier,
    call: Call,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle,
        )
    },
) {
    var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

    Box(modifier = modifier.background(color = VideoTheme.colors.appBackground)) {
        val roomParticipants by call.state.participants.collectAsStateWithLifecycle()

        if (roomParticipants.isNotEmpty()) {
            OrientationVideoRenderer(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { parentSize = it },
                call = call,
                parentSize = parentSize,
                style = style,
                videoRenderer = videoRenderer,
            )
        }
    }
}

@Preview
@Composable
private fun RegularCallVideoRendererPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantsRegularGrid(
            call = mockCall,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
