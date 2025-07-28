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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.getstream.video.android.core.RealtimeConnection
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
 * @param liveStreamErrorContent UI shown in case of an error joining or rendering the livestream.
 * @param onRetryJoin Callback triggered when retrying to join the livestream after a failure.
 *
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
    rendererContent: @Composable BoxScope.(Call) -> Unit = defaultRenderer,
    overlayContent: @Composable BoxScope.(Call) -> Unit = defaultLivestreamPlayerOverlay,
    liveStreamEndedContent: @Composable BoxScope.(Call) -> Unit = {
        LivestreamEndedUi(call)
    },
    liveStreamHostVideoNotAvailableContent: @Composable BoxScope.(Call) -> Unit = {
        HostVideoNotAvailableUi(call)
    },
    liveStreamErrorContent: @Composable BoxScope.(Call, () -> Unit) -> Unit = { _, retryJoin ->
        LivestreamErrorUi(call, retryJoin)
    },
    onRetryJoin: () -> Unit = {},
) {
    val (isAudioEnabled, onAudioToggle) = rememberAudioToggleState(call)

    val wrappedRenderer = wrapRendererContent(
        call = call,
        enablePausing = enablePausing,
        onPausedPlayer = onPausedPlayer,
        videoRendererConfig = videoRendererConfig,
        livestreamFlow = livestreamFlow,
        onAudioToggle = onAudioToggle,
        rendererContent = rendererContent,
    )

    val wrappedOverlay = wrapOverlayContent(
        call = call,
        isAudioEnabled = isAudioEnabled,
        onAudioToggle = onAudioToggle,
        overlayContent = overlayContent,
    )

    val livestream by livestreamFlow.collectAsStateWithLifecycle(initialValue = null)
    val hostVideoAvailable = livestream?.enabled == true

    val connection by call.state.connection.collectAsStateWithLifecycle()
    val endedAt by call.state.endedAt.collectAsStateWithLifecycle()
    val backstage by call.state.backstage.collectAsStateWithLifecycle()
    var hasJoinedSuccessfully by rememberSaveable { mutableStateOf(false) }
    var livestreamState by rememberSaveable { mutableStateOf(LivestreamState.INITIAL) }

    LaunchedEffect(connection, endedAt, backstage) {
        when (connection) {
            is RealtimeConnection.Connected -> {
                hasJoinedSuccessfully = true
                livestreamState = when {
                    endedAt != null -> LivestreamState.ENDED
                    backstage -> LivestreamState.BACKSTAGE
                    else -> LivestreamState.LIVE
                }
            }

            is RealtimeConnection.Failed -> livestreamState = LivestreamState.ERROR
            is RealtimeConnection.InProgress -> livestreamState = LivestreamState.JOINING
            else -> { }
        }
    }

    LivestreamPlayerImpl(
        modifier = modifier,
        call = call,
        enablePausing = enablePausing,
        onPausedPlayer = onPausedPlayer,
        backstageContent = backstageContent,
        videoRendererConfig = videoRendererConfig,
        livestreamFlow = livestreamFlow,
        hostVideoAvailable = hostVideoAvailable,
        livestreamState = livestreamState,
        rendererContent = wrappedRenderer,
        overlayContent = wrappedOverlay,
        liveStreamEndedContent = liveStreamEndedContent,
        liveStreamHostVideoNotAvailableContent = liveStreamHostVideoNotAvailableContent,
        liveStreamErrorContent = liveStreamErrorContent,
        onRetryJoin = onRetryJoin,
    )
}

@Composable
internal fun LivestreamPlayerImpl(
    modifier: Modifier = Modifier,
    call: Call,
    enablePausing: Boolean,
    onPausedPlayer: ((isPaused: Boolean) -> Unit)?,
    backstageContent: @Composable BoxScope.(Call) -> Unit,
    videoRendererConfig: VideoRendererConfig,
    livestreamFlow: Flow<ParticipantState.Video?>,
    hostVideoAvailable: Boolean,
    livestreamState: LivestreamState,
    rendererContent: @Composable BoxScope.(Call) -> Unit,
    overlayContent: @Composable BoxScope.(Call) -> Unit,
    liveStreamEndedContent: @Composable BoxScope.(Call) -> Unit,
    liveStreamHostVideoNotAvailableContent: @Composable BoxScope.(
        Call,
    ) -> Unit,
    liveStreamErrorContent: @Composable BoxScope.(
        Call,
        () -> Unit,
    ) -> Unit,
    onRetryJoin: () -> Unit,
) {
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

            LivestreamState.JOINING -> {
            }

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

@Composable
private fun rememberAudioToggleState(call: Call): Pair<Boolean, (Boolean) -> Unit> {
    var isAudioEnabled by rememberSaveable { mutableStateOf(true) }

    val onAudioToggle: (Boolean) -> Unit = remember(call) {
        {
                enabled ->
            call.setIncomingAudioEnabled(enabled)
            isAudioEnabled = enabled
        }
    }

    return isAudioEnabled to onAudioToggle
}

@Composable
private fun wrapRendererContent(
    call: Call,
    enablePausing: Boolean,
    onPausedPlayer: ((isPaused: Boolean) -> Unit)? = {},
    videoRendererConfig: VideoRendererConfig,
    livestreamFlow: Flow<ParticipantState.Video?>,
    onAudioToggle: (Boolean) -> Unit,
    rendererContent: @Composable BoxScope.(Call) -> Unit,
): @Composable BoxScope.(Call) -> Unit {
    return if (rendererContent === defaultRenderer) {
        {
            LivestreamRenderer(
                call = call,
                enablePausing = enablePausing,
                onPausedPlayer = onPausedPlayer,
                configuration = videoRendererConfig,
                livestreamFlow = livestreamFlow,
                onAudioToggle = onAudioToggle,
            )
        }
    } else {
        rendererContent
    }
}

@Composable
private fun wrapOverlayContent(
    call: Call,
    isAudioEnabled: Boolean,
    onAudioToggle: (Boolean) -> Unit,
    overlayContent: @Composable BoxScope.(Call) -> Unit,
): @Composable BoxScope.(Call) -> Unit {
    return if (overlayContent === defaultLivestreamPlayerOverlay) {
        {
            LivestreamPlayerOverlay(
                call = call,
                isAudioEnabled = isAudioEnabled,
                onAudioToggle = onAudioToggle,
            )
        }
    } else {
        overlayContent
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

private val defaultRenderer: @Composable BoxScope.(Call) -> Unit = { call ->
    LivestreamRenderer(
        call = call,
        enablePausing = true,
    )
}
private val defaultLivestreamPlayerOverlay: @Composable BoxScope.(Call) -> Unit = { call ->
    LivestreamPlayerOverlay(call = call)
}
