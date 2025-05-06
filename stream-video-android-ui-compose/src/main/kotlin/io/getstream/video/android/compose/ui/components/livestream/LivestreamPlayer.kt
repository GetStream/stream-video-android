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

package io.getstream.video.android.compose.ui.components.livestream

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.config.VideoRendererConfig
import io.getstream.video.android.compose.ui.components.video.config.videoRenderConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Represents livestreaming content based on the call state provided from the [call].

 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param enablePausing Enables pausing or resuming the livestream video.
 * @param onPausedPlayer Listen to pause or resume the livestream video.
 * @param backstageContent Content shown when the host has not yet started the live stream.
 * @param videoRendererConfig Configuration for the internal [VideoRenderer]
 * @param rendererContent The rendered stream originating from the host.
 * @param overlayContent Content displayed to indicate participant counts, live stream duration, and device settings controls.
 */
@Composable
public fun LivestreamPlayer(
    modifier: Modifier = Modifier,
    call: Call,
    enablePausing: Boolean = true,
    onPausedPlayer: ((isPaused: Boolean) -> Unit)? = {},
    backstageContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamBackStage()
    },
    videoRendererConfig: VideoRendererConfig = videoRenderConfig(),
    livestreamFlow: Flow<ParticipantState.Video?> =
        call.state.participants.flatMapLatest { participants: List<ParticipantState> ->
            // For each participant, create a small Flow that watches videoEnabled.
            val participantVideoFlows = participants.map { participant ->
                participant.videoEnabled.map { enabled -> participant to enabled }
            }
            // Combine these Flows: whenever a participant’s videoEnabled changes,
            // we re-calculate which participants have video.
            combine(participantVideoFlows) { participantEnabledPairs ->
                participantEnabledPairs
                    .filter { (_, isEnabled) -> isEnabled }
                    .map { (participant, _) -> participant }
            }
        }.flatMapLatest { participantWithVideo ->
            participantWithVideo.firstOrNull()?.video ?: flow { emit(null) }
        },
    rendererContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamRenderer(
            call = call,
            enablePausing = enablePausing,
            onPausedPlayer = onPausedPlayer,
            configuration = videoRendererConfig,
            livestreamFlow = livestreamFlow,
        )
    },
    overlayContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamPlayerOverlay(call = call)
    },
) {
    val backstage by call.state.backstage.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (backstage) {
            backstageContent.invoke(this, call)
        } else {
            rendererContent.invoke(this, call)

            overlayContent.invoke(this, call)
        }
    }
}

@Preview
@Composable
private fun LivestreamPlayerPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LivestreamPlayer(call = previewCall)
    }
}
