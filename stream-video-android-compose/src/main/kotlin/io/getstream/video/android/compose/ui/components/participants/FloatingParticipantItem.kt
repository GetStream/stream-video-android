package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState

@Composable
public fun FloatingParticipantItem(
    call: Call,
    localParticipant: CallParticipantState,
    parentBounds: Rect,
    modifier: Modifier = Modifier
) {
    var videoSize by remember { mutableStateOf(IntSize(0, 0)) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val offset by animateOffsetAsState(targetValue = Offset(offsetX, offsetY))

    val density = LocalDensity.current
    val bottomOffset = density.run { VideoTheme.dimens.callControlsSheetHeight.toPx() }
    val paddingOffset = density.run { VideoTheme.dimens.floatingVideoPadding.toPx() }

    val track = localParticipant.videoTrack

    if (track != null) {
        Card(
            elevation = 8.dp,
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val newOffsetX = (offsetX + dragAmount.x)
                            .coerceAtLeast(0f)
                            .coerceAtMost(parentBounds.width - videoSize.width - paddingOffset * 2)

                        val newOffsetY = (offsetY + dragAmount.y)
                            .coerceAtLeast(0f)
                            .coerceAtMost(parentBounds.bottom - videoSize.height - bottomOffset)

                        offsetX = newOffsetX
                        offsetY = newOffsetY
                    }
                }
                .then(modifier)
                .padding(VideoTheme.dimens.floatingVideoPadding)
                .onGloballyPositioned { videoSize = it.size },
            shape = RoundedCornerShape(16.dp)
        ) {
            VideoRenderer(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                call = call,
                videoTrack = track
            )
        }
    }
}