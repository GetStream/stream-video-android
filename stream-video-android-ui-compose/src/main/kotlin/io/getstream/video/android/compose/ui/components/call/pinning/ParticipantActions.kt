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

package io.getstream.video.android.compose.ui.components.call.pinning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.lifecycleScope
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.indicator.GenericIndicator
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
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
    public val icon: ImageVector,
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
        icon = Icons.Outlined.PushPin,
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
        icon = Icons.Filled.PushPin,
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
        icon = Icons.Outlined.PushPin,
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
        icon = Icons.Filled.PushPin,
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
private fun BoxScope.ParticipantActionsWithoutState(
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
            backgroundColor = VideoTheme.colors.baseSheetPrimary,
            shape = VideoTheme.shapes.circle,
            modifier = modifier.clickable {
                onClick()
            }.onGloballyPositioned { coordinates ->
                buttonPosition.value = coordinates.positionInParent()
                buttonSize.value = coordinates.size
            }.clip(VideoTheme.shapes.circle),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "Call actions",
                tint = VideoTheme.colors.basePrimary,
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
    val coroutineScope = LocalLifecycleOwner.current.lifecycleScope
    Popup(
        offset = offset,
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .background(VideoTheme.colors.baseSheetPrimary, shape = VideoTheme.shapes.dialog)
                .align(Center)
                .width(220.dp),
        ) {
            actions.forEach {
                if (it.condition(call, participant)) {
                    StreamToggleButton(
                        modifier = Modifier.width(220.dp),
                        toggleState = rememberUpdatedState(
                            newValue = ToggleableState(!it.firstToggleAction),
                        ),
                        onIcon = it.icon,
                        onText = it.label,
                        offText = it.label,
                        onStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOn(),
                        offStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOff(),
                    ) { _ ->
                        it.action.invoke(coroutineScope, call, participant)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ParticipantActionDialogPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsDialog(
                call = previewCall,
                participant = previewParticipant,
                actions = participantActions,
                offset = IntOffset(
                    x = 0,
                    y = 50,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantActionsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsWithoutState(
                actions = participantActions,
                call = previewCall,
                participant = previewParticipant,
                showDialog = true,
            ) {
            }
        }
    }
}

@Preview
@Composable
private fun ParticipantActionsKickPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsWithoutState(
                actions = participantActions,
                call = previewCall,
                participant = previewParticipant,
                showDialog = true,
            ) {
            }
        }
    }
}
