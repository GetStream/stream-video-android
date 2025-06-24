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
import io.getstream.video.android.compose.ui.components.livestream.state.LivestreamState
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
 * Renders a livestream video player UI based on the current state of the provided [call].
 *
 * This composable adapts its content based on the [livestreamState], displaying backstage,
 * live, ended, or error views accordingly. It supports pause/resume controls, retry logic,
 * and customizable UI slots for various livestream stages.
 *
 * @param modifier Modifier used to style the livestream container.
 * @param call The call instance providing state and media tracks for rendering.
 * @param enablePausing Whether the livestream video can be paused and resumed by the viewer.
 * @param onPausedPlayer Callback to observe pause/resume interactions.
 * @param backstageContent UI shown when the host hasn't started the livestream.
 * @param videoRendererConfig Configuration for how the video is rendered internally.
 * @param livestreamFlow A Flow emitting the video track of the host or primary speaker.
 * @param rendererContent UI responsible for rendering the host’s video stream.
 * @param overlayContent UI layered on top of the video for participant counts, controls, etc.
 * @param liveStreamEndedContent UI shown when the livestream has ended.
 * @param liveStreamHostVideoNotAvailableContent UI shown when the host has no video available.
 * @param onRetryJoin Callback triggered when retrying to join the livestream after a failure.
 * @param liveStreamErrorContent UI shown in case of an error joining or rendering the livestream.
 * @param livestreamState The current state of the livestream (e.g. BACKSTAGE, LIVE, ENDED).
 */

@Composable
public fun LivestreamPlayer(
    modifier: Modifier = Modifier,
    call: Call,
    enablePausing: Boolean = true,
    onPausedPlayer: ((isPaused: Boolean) -> Unit)? = {},
    backstageContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamBackStage(call)
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
    liveStreamEndedContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamEndedUi(call)
    },
    liveStreamHostVideoNotAvailableContent: @Composable BoxScope.(Call) -> Unit = {
        HostVideoNotAvailableUi(call)
    },
    onRetryJoin: () -> Unit = {},
    liveStreamErrorContent: @Composable BoxScope.(Call, () -> Unit) -> Unit = { _, _ ->
        LivestreamErrorUi(call, onRetryJoin)
    },
    livestreamState: LivestreamState = LivestreamState.INITIAL,
) {
    val livestream by livestreamFlow.collectAsStateWithLifecycle(initialValue = null)
    val hostVideoAvailable = livestream?.enabled == true

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (livestreamState) {
            LivestreamState.INITIAL -> {
                LivestreamPlayerCompatibilityContent(
                    call,
                    backstageContent = backstageContent,
                    rendererContent = rendererContent,
                    overlayContent = overlayContent,
                )
            }

            LivestreamState.JOINING -> {}

            LivestreamState.BACKSTAGE -> {
                backstageContent.invoke(this, call)
            }

            LivestreamState.LIVE -> {
                if (hostVideoAvailable) {
                    rendererContent.invoke(this, call)
                } else {
                    liveStreamHostVideoNotAvailableContent.invoke(this, call)
                }
                overlayContent.invoke(this, call)
            }

            LivestreamState.ERROR -> {
                liveStreamErrorContent.invoke(this, call, onRetryJoin)
            }

            LivestreamState.ENDED -> {
                liveStreamEndedContent.invoke(this, call)
            }
        }
    }
}

/**
 * Renders a livestream video player UI based on the current state of the provided [call].
 *
 * @param modifier Modifier used to style the livestream container.
 * @param call The call instance providing state and media tracks for rendering.
 * @param enablePausing Whether the livestream video can be paused and resumed by the viewer.
 * @param onPausedPlayer Callback to observe pause/resume interactions.
 * @param backstageContent UI shown when the host hasn't started the livestream.
 * @param videoRendererConfig Configuration for how the video is rendered internally.
 * @param livestreamFlow A Flow emitting the video track of the host or primary speaker.
 * @param rendererContent UI responsible for rendering the host’s video stream.
 * @param overlayContent UI layered on top of the video for participant counts, controls, etc.
 * @param liveStreamEndedContent UI shown when the livestream has ended.
 * @param liveStreamHostVideoNotAvailableContent UI shown when the host has no video available.
 * @param liveStreamErrorContent UI shown in case of an error joining or rendering the livestream.
 */

@Deprecated(
    message = "This LivestreamPlayer overload is deprecated. Please use the newer API that includes 'livestreamState' and 'onRetryJoin' parameters for better state handling and retry support.",
    replaceWith = ReplaceWith(
        expression = "LivestreamPlayer(call = call, livestreamState = LivestreamState.INITIAL, onRetryJoin = { /* retry logic */ })",
    ),
    level = DeprecationLevel.WARNING,
)
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
    liveStreamEndedContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamEndedUi(call)
    },
    liveStreamHostVideoNotAvailableContent: @Composable BoxScope.(Call) -> Unit = {
        HostVideoNotAvailableUi(call)
    },
    liveStreamErrorContent: @Composable BoxScope.(Call, () -> Unit) -> Unit = { _, onRetry ->
        LivestreamErrorUi(call, onRetry)
    },
) {
    LivestreamPlayer(
        modifier = modifier,
        call = call,
        enablePausing = enablePausing,
        onPausedPlayer = onPausedPlayer,
        backstageContent = backstageContent,
        videoRendererConfig = videoRendererConfig,
        livestreamFlow = livestreamFlow,
        rendererContent = rendererContent,
        overlayContent = overlayContent,
        liveStreamEndedContent = liveStreamEndedContent,
        liveStreamHostVideoNotAvailableContent = liveStreamHostVideoNotAvailableContent,
        onRetryJoin = {},
        liveStreamErrorContent = liveStreamErrorContent,
        livestreamState = LivestreamState.INITIAL,
    )
}

@Composable
private fun BoxScope.LivestreamPlayerCompatibilityContent(
    call: Call,
    backstageContent: @Composable BoxScope.(Call) -> Unit,
    rendererContent: @Composable BoxScope.(Call) -> Unit,
    overlayContent: @Composable BoxScope.(Call) -> Unit,
) {
    val backstage by call.state.backstage.collectAsStateWithLifecycle()

    if (backstage) {
        backstageContent.invoke(this, call)
    } else {
        rendererContent.invoke(this, call)

        overlayContent.invoke(this, call)
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
