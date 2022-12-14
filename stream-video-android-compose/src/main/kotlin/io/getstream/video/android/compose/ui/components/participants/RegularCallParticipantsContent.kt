package io.getstream.video.android.compose.ui.components.participants

import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.internal.Participants
import io.getstream.video.android.model.Call

@Composable
public fun RegularCallParticipantsContent(
    call: Call,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRender: (View) -> Unit = {}
) {
    var bounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    BoxWithConstraints(
        modifier = modifier
            .padding(paddingValues)
            .onGloballyPositioned {
                bounds = it.boundsInParent()
            }
    ) {
        val roomParticipants by call.callParticipants.collectAsState(emptyList())
        val participants = roomParticipants.filter { !it.isLocal }.distinctBy { it.id }

        val localParticipantState by call.localParticipant.collectAsState(initial = null)
        val currentLocal = localParticipantState

        if (participants.isNotEmpty()) {
            Participants(
                modifier = Modifier.fillMaxSize(),
                call = call,
                onRender = onRender
            )

            if (currentLocal != null) {
                FloatingParticipantItem(
                    call = call,
                    localParticipant = currentLocal,
                    parentBounds = bounds,
                    modifier = Modifier
                        .size(
                            height = VideoTheme.dimens.floatingVideoHeight,
                            width = VideoTheme.dimens.floatingVideoWidth
                        )
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        } else if (currentLocal?.videoTrack?.video != null) {
            CallParticipant(call = call, participant = currentLocal)
        }
    }
}