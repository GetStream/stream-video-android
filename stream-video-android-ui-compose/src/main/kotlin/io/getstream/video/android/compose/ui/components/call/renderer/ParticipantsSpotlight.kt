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

package io.getstream.video.android.compose.ui.components.call.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.ui.components.call.renderer.internal.SpotlightVideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState

/**
 * Renders all the CallParticipants, based on the number of people in a call and the call state.
 * Also takes into account if there are any screen sharing sessions active and adjusts the UI
 * accordingly.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param isZoomable Decide to this screensharing video renderer is zoomable or not.
 * @param style Defined properties for styling a single video call track.
 * @param videoRenderer A single video renderer renders each individual participant.
 */
@Composable
public fun ParticipantsSpotlight(
    call: Call,
    modifier: Modifier = Modifier,
    isZoomable: Boolean = false,
    style: VideoRendererStyle = SpotlightVideoRendererStyle(),
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
    val configuration = LocalConfiguration.current
    val participants by call.state.participants.collectAsStateWithLifecycle(emptyList())
    val dominantSpeaker by call.state.dominantSpeaker.collectAsStateWithLifecycle()
    val pinnedParticipant by call.state.pinnedParticipants.collectAsStateWithLifecycle()
    val speaker by remember(key1 = dominantSpeaker, key2 = pinnedParticipant, key3 = participants) {
        derivedStateOf {
            val pinnedSpeakerId = pinnedParticipant.keys.firstOrNull()
            val pinnedSpeaker = participants.find { it.sessionId == pinnedSpeakerId }
            pinnedSpeaker ?: dominantSpeaker ?: participants.firstOrNull()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Either the dominant speaker, or the first participant in the spotlight
        SpotlightVideoRenderer(
            call = call,
            speaker = speaker,
            participants = participants,
            orientation = configuration.orientation,
            modifier = modifier.fillMaxSize().testTag("Stream_SpotlightView"),
            isZoomable = isZoomable,
            style = style,
            videoRenderer = videoRenderer,
        )
    }
}
