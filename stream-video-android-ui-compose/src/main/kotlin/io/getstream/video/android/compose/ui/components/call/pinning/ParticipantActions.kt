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

package io.getstream.video.android.compose.ui.components.call.pinning

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.lifecycleScope
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.compose.ui.components.base.StreamListItem
import io.getstream.video.android.compose.ui.components.indicator.GenericIndicator
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Defines a participant action.
 *
 * @param icon the icon that represents the action.
 * @param label the text that represents the action.
 * @param firstToggleAction a boolean noting if this action is first of two (i.e. Pin is first = true, where Unpin is first=false)
 * @param condition the condition if the action is to be shown or not.
 * @param action the action (i.e. callable)
 */
public class ParticipantAction(
    @DrawableRes public val icon: Int,
    public val label: String,
    public val firstToggleAction: Boolean = true,
    public val condition: (Call, ParticipantState) -> Boolean = { _, _ -> true },
    public val action: CoroutineScope.(Call, ParticipantState) -> Unit = { _, _ -> },
)

/**
 * Default actions representing local and server side pin/unpin.
 */
internal val participantActions: List<ParticipantAction> = listOf(
    ParticipantAction(
        icon = R.drawable.stream_design_ic_pin,
        label = "Pin",
        condition = { call, participantState ->
            !call.isLocalPin(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.state.pin(participantState.userId.value, participantState.sessionId)
            }
        },
    ),
    ParticipantAction(
        icon = R.drawable.stream_design_ic_pin_fill,
        label = "Unpin",
        firstToggleAction = false,
        condition = { call, participantState ->
            call.isLocalPin(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.state.unpin(participantState.sessionId)
            }
        },
    ),
    ParticipantAction(
        icon = R.drawable.stream_design_ic_pin,
        label = "Pin for everyone",
        condition = { call, participantState ->
            call.hasCapability(OwnCapability.PinForEveryone) && !call.isServerPin(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.pinForEveryone(participantState.sessionId, participantState.userId.value)
            }
        },
    ),
    ParticipantAction(
        icon = R.drawable.stream_design_ic_pin_fill,
        label = "Unpin for everyone",
        firstToggleAction = false,
        condition = { call, participantState ->
            call.hasCapability(OwnCapability.PinForEveryone) && call.isServerPin(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.unpinForEveryone(participantState.sessionId, participantState.userId.value)
            }
        },
    ),
)

/**
 * Renders a set of actions for a given participant.
 *
 * @param modifier Modifier for styling.
 * @param actions A list of actions to render.
 * @param call The call that contains all the participants state and tracks.
 * @param participant The participant to render actions for.
 */
@InternalStreamVideoApi
@Composable
public fun BoxScope.ParticipantActions(
    modifier: Modifier = Modifier,
    actions: List<ParticipantAction>,
    call: Call,
    participant: ParticipantState,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    ParticipantActionsWithoutState(actions, call, participant, modifier, showDialog) {
        showDialog = !showDialog
    }
}

@Composable
internal fun BoxScope.ParticipantActionsWithoutState(
    actions: List<ParticipantAction>,
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    showDialog: Boolean = false,
    onClick: () -> Unit = {},
) {
    val buttonPosition = remember { mutableStateOf(Offset.Zero) }
    val buttonSize = remember { mutableStateOf(IntSize.Zero) }
    if (actions.any {
            it.condition.invoke(call, participant)
        }
    ) {
        GenericIndicator(
            backgroundColor = VideoTheme.colors.backgroundCoreApp,
            shape = CircleShape,
            modifier = modifier.clickable {
                onClick()
            }.onGloballyPositioned { coordinates ->
                buttonPosition.value = coordinates.positionInParent()
                buttonSize.value = coordinates.size
            }.clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.stream_design_ic_more_horizontal),
                contentDescription = "Call actions",
                tint = VideoTheme.colors.textPrimary,
            )
        }

        if (showDialog) {
            ParticipantActionsDialog(
                offset = IntOffset(
                    x = buttonPosition.value.x.toInt(),
                    y = (buttonPosition.value.y + buttonSize.value.height).toInt(),
                ),
                call = call,
                participant = participant,
                actions = actions,
                onDismiss = {
                    onClick()
                },
            )
        }
    }
}

@Composable
internal fun BoxScope.ParticipantActionsDialog(
    call: Call,
    participant: ParticipantState,
    actions: List<ParticipantAction>,
    onDismiss: () -> Unit = {},
    offset: IntOffset,
) {
    Popup(
        offset = offset,
        onDismissRequest = onDismiss,
    ) {
        ParticipantActionsDialogContent(
            call = call,
            participant = participant,
            actions = actions,
            onDismiss = onDismiss,
        )
    }
}

@Composable
internal fun BoxScope.ParticipantActionsDialogContent(
    call: Call,
    participant: ParticipantState,
    actions: List<ParticipantAction>,
    onDismiss: () -> Unit = {},
) {
    val coroutineScope = LocalLifecycleOwner.current.lifecycleScope
    Column(
        Modifier
            .background(
                VideoTheme.colors.backgroundCoreElevation1,
                shape = RoundedCornerShape(StreamTokens.radiusLg),
            )
            .align(Center)
            .width(220.dp),
    ) {
        actions.forEach {
            if (it.condition(call, participant)) {
                StreamListItem(
                    title = it.label,
                    onClick = {
                        it.action.invoke(coroutineScope, call, participant)
                        onDismiss()
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(it.icon),
                            contentDescription = null,
                            modifier = Modifier.size(StreamTokens.iconSizeMd),
                        )
                    },
                )
            }
        }
    }
}
