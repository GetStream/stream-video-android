package io.getstream.video.android.compose.ui.components.call.renderer.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantsGrid
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable


@Composable
internal fun SpotlightVideoRenderer(
    call: Call,
    orientation: Int,
    modifier: Modifier,
    isZoomable: Boolean,
    style: VideoRendererStyle,
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
    }
) {
    val participants by call.state.participants.collectAsStateWithLifecycle()
    val dominantSpeaker by call.state.dominantSpeaker.collectAsStateWithLifecycle()

    val speaker = dominantSpeaker ?: participants.firstOrNull()

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val gridState = lazyGridStateWithVisibilityNotification(call = call)
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Fixed(2),
                state = gridState,
                content = {
                    item(
                        key = "spotlight",
                        span = { GridItemSpan(2) }
                    ) {
                        SpotlightParticipantRenderer(
                            participant = dominantSpeaker,
                            call = call,
                            isZoomable = isZoomable,
                            style = style,
                            videoRenderer = videoRenderer
                        )
                    }
                    items(
                        count = participants.size,
                        key = { participants[it].sessionId },
                    ) { key ->
                        val itemHeight = with(LocalDensity.current) {
                            (constraints.maxHeight / 6).toDp()
                        }
                        val participant = participants[key]
                        videoRenderer.invoke(
                            modifier = modifier.height(itemHeight),
                            call = call,
                            participant = participant,
                            style = style.copy(
                                isFocused = dominantSpeaker?.sessionId == participant.sessionId,
                            ),
                        )
                    }
                },
            )
        }
}

@Composable
internal fun SpotlightParticipantRenderer(call: Call,
                                 participant: ParticipantState?,
                                 modifier: Modifier = Modifier,
                                 style: VideoRendererStyle,
                                 labelPosition: Alignment = Alignment.BottomStart,
                                 isShowConnectionQualityIndicator: Boolean = true,
                                 isZoomable: Boolean = true,
                                 videoRenderer: @Composable (modifier: Modifier, call: Call, participant: ParticipantState, style: VideoRendererStyle) -> Unit
) {
    val zoomableState = rememberZoomableState()
    Box(modifier = modifier
        .fillMaxWidth()
        .zoomable(
            state = zoomableState,
            enabled = isZoomable,
        )
    ) {
        if (participant != null) {
            videoRenderer.invoke(Modifier.fillMaxSize(), call, participant, style)
        } else {
            Box(modifier = Modifier
                .fillMaxHeight(0.4f)
                .background(VideoTheme.colors.appBackground)
            )
        }
    }
}
