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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
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
internal val pinUnpinActions: List<ParticipantAction> = listOf(
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
        nameValue.ifEmpty {
            participant.userNameOrId.value
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
                        val circleColor = if (it.firstToggleAction) VideoTheme.colors.textHighEmphasis else VideoTheme.colors.primaryAccent
                        val strokeWidth = if (it.firstToggleAction) 2.dp else 4.dp
                        Column {
                            CircleIcon(
                                icon = it.icon,
                                modifier = Modifier.align(CenterHorizontally),
                                tint = circleColor,
                                circleColor = circleColor,
                                strokeWidth = strokeWidth,
                            ) {
                                it.action.invoke(coroutineScope, call, participant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.align(CenterHorizontally).width(80.dp),
                                textAlign = TextAlign.Center,
                                text = it.label,
                                color = VideoTheme.colors.textHighEmphasis,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CircleIcon(
    icon: ImageVector,
    modifier: Modifier,
    tint: Color,
    circleColor: Color,
    strokeWidth: Dp = 2.dp,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable {
                onClick()
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            drawCircleOutline(density, circleColor, strokeWidth)
        })
        Icon(
            modifier = Modifier.align(Center),
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
    }
}

private fun DrawScope.drawCircleOutline(density: Density, color: Color, width: Dp) {
    val strokeWidth = with(density) { width.toPx() }
    drawCircle(
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth),
    )
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
