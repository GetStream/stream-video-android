/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.indicator.GenericIndicator
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openapitools.client.models.OwnCapability

/**
 * Defines a participant action.
 *
 * @param icon the icon that represents the action.
 * @param label the text that represents the action.
 * @param condition the condition if the action is to be shown or not.
 * @param action the action (i.e. callable)
 */
public class ParticipantAction(
    public val icon: ImageVector? = null,
    public val label: String,
    public val condition: (Call, ParticipantState) -> Boolean = { _, _ -> true },
    public val action: CoroutineScope.(Call, ParticipantState) -> Unit = { _, _ -> },
)

/**
 * Default actions representing local and server side pin/unpin.
 */
internal val pinUnpinActions: List<ParticipantAction> = listOf(
    ParticipantAction(
        icon = Icons.Filled.PushPin,
        label = "Pin",
        condition = { call, participantState ->
            !call.isPinnedParticipant(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.state.pin(participantState.sessionId)
            }
        },
    ),
    ParticipantAction(
        icon = Icons.Filled.PushPin,
        label = "Pin for everyone",
        condition = { call, participantState ->
            call.hasCapability(OwnCapability.PinForEveryone) && !call.isPinnedParticipant(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.state.pin(participantState.sessionId)
                call.pinForEveryone(call.type, participantState.sessionId)
            }
        },
    ),
    ParticipantAction(
        icon = Icons.Filled.Cancel,
        label = "Unpin for everyone",
        condition = { call, participantState ->
            call.hasCapability(OwnCapability.PinForEveryone) && call.isPinnedParticipant(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.state.unpin(participantState.sessionId)
                call.unpinForEveryone(call.type, participantState.sessionId)
            }
        },
    ),
    ParticipantAction(
        icon = Icons.Filled.Cancel,
        label = "Unpin",
        condition = { call, participantState ->
            call.isPinnedParticipant(participantState.sessionId)
        },
        action = { call, participantState ->
            launch {
                call.state.unpin(participantState.sessionId)
            }
        },
    ),
)

@Composable
internal fun BoxScope.ParticipantActions(
    modifier: Modifier = Modifier,
    actions: List<ParticipantAction>,
    call: Call,
    participant: ParticipantState,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    if (actions.any {
            it.condition.invoke(call, participant)
        }
    ) {
        GenericIndicator(
            modifier = modifier.clickable {
                showDialog = !showDialog
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "Call actions",
                tint = Color.White,
            )
        }

        if (showDialog) {
            ParticipantActionsDialog(
                call = call,
                participant = participant,
                actions = actions,
                onDismiss = {
                    showDialog = false
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BoxScope.ParticipantActionsDialog(
    call: Call,
    participant: ParticipantState,
    actions: List<ParticipantAction>,
    onDismiss: () -> Unit = {},
) {
    val coroutineScope = LocalLifecycleOwner.current.lifecycleScope
    val userName by participant.userNameOrId.collectAsStateWithLifecycle()
    val userImage by participant.image.collectAsStateWithLifecycle()
    val name = remember {
        val nameValue = participant.name.value
        if (nameValue.isEmpty()) {
            participant.userNameOrId.value
        } else {
            nameValue
        }
    }
    Dialog(onDismiss) {
        Column(
            Modifier
                .background(VideoTheme.colors.appBackground)
                .align(Alignment.Center)
                .padding(16.dp),
        ) {
            UserAvatar(
                modifier = Modifier
                    .size(82.dp)
                    .align(Alignment.CenterHorizontally)
                    .aspectRatio(1f),
                userName = userName,
                userImage = userImage,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = name,
                color = VideoTheme.colors.textHighEmphasis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.Center,
            ) {
                actions.forEach {
                    if (it.condition.invoke(call, participant)) {
                        Button(
                            modifier = Modifier.padding(8.dp),
                            onClick = {
                                it.action.invoke(coroutineScope, call, participant)
                            },
                        ) {
                            it.icon?.let { hasIcon ->
                                Icon(
                                    imageVector = hasIcon,
                                    contentDescription = it.label,
                                )
                            }
                            Text(
                                textAlign = TextAlign.Start,
                                text = it.label,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                actions = pinUnpinActions,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ParticipantActionDialogPreviewDark() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantActionsDialog(
                call = previewCall,
                participant = previewParticipant,
                actions = pinUnpinActions,
            )
        }
    }
}
