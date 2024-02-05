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

@file:OptIn(ExperimentalMaterialApi::class)

package io.getstream.video.android.ui.call

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.chat.android.ui.common.state.messages.list.MessageItemState
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamBadgeBox
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.ChatDialogAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSettingsAction
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.call.state.ChooseLayout
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.tooling.util.StreamFlavors
import io.getstream.video.android.util.config.AppConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openapitools.client.models.OwnCapability

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CallScreen(
    call: Call,
    showDebugOptions: Boolean = false,
    onCallDisconnected: () -> Unit = {},
    onUserLeaveCall: () -> Unit = {},
) {
    val context = LocalContext.current
    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val speakingWhileMuted by call.state.speakingWhileMuted.collectAsStateWithLifecycle()
    var isShowingSettingMenu by remember { mutableStateOf(false) }
    var isShowingLayoutChooseMenu by remember { mutableStateOf(false) }
    var isShowingReactionsMenu by remember { mutableStateOf(false) }
    var isShowingAvailableDeviceMenu by remember { mutableStateOf(false) }
    var isBackgroundBlurEnabled by remember { mutableStateOf(false) }
    var isShowingStats by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf(LayoutType.DYNAMIC) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var showParticipants by remember { mutableStateOf(false) }
    val chatState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    var showRecordingWarning by remember {
        mutableStateOf(false)
    }
    var showEndRecordingDialog by remember { mutableStateOf(false) }
    var acceptedCallRecording by remember { mutableStateOf(false) }
    val isRecording by call.state.recording.collectAsStateWithLifecycle()
    val participantsSize by call.state.participants.collectAsStateWithLifecycle()
    val messages: MutableList<MessageItemState> = remember { mutableStateListOf() }
    var messagesVisibility by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messageScope = rememberCoroutineScope()

    val connection by call.state.connection.collectAsStateWithLifecycle()
    val me by call.state.me.collectAsState()

    LaunchedEffect(key1 = connection) {
        if (connection == RealtimeConnection.Disconnected) {
            onCallDisconnected.invoke()
        } else if (connection is RealtimeConnection.Failed) {
            Toast.makeText(
                context,
                "Call connection failed (${(connection as RealtimeConnection.Failed).error}",
                Toast.LENGTH_LONG,
            ).show()
            onCallDisconnected.invoke()
        }
    }

    VideoTheme {
        ChatDialog(
            state = chatState,
            call = call,
            content = {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    CallContent(
                        modifier = Modifier
                            .background(
                                color = VideoTheme.colors.baseSheetPrimary,
                            )
                            .padding(vertical = VideoTheme.dimens.genericL),
                        call = call,
                        layout = layout,
                        enableInPictureInPicture = true,
                        enableDiagnostics = BuildConfig.DEBUG || BuildConfig.FLAVOR == StreamFlavors.development,
                        onCallAction = {
                            when (it) {
                                ChooseLayout -> isShowingLayoutChooseMenu = true
                                else -> DefaultOnCallActionHandler.onCallAction(
                                    call,
                                    it,
                                )
                            }
                        },
                        appBarContent = {
                            CallAppBar(
                                call = call,
                                leadingContent = {
                                    val iconOnOff = ImageVector.vectorResource(
                                        R.drawable.ic_layout_grid,
                                    )
                                    Row {
                                        ToggleAction(
                                            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                                            isActionActive = !isShowingLayoutChooseMenu,
                                            iconOnOff = Pair(iconOnOff, iconOnOff),
                                        ) {
                                            isShowingLayoutChooseMenu = !isShowingLayoutChooseMenu
                                        }

                                        Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))

                                        FlipCameraAction(
                                            onCallAction = { call.camera.flip() },
                                        )
                                    }
                                },
                                trailingContent = {
                                    LeaveCallAction {
                                        call.leave()
                                    }
                                },
                            )
                        },
                        onBackPressed = {
                            if (chatState.currentValue == ModalBottomSheetValue.Expanded) {
                                scope.launch { chatState.hide() }
                            } else {
                                onUserLeaveCall.invoke()
                            }
                        },
                        controlsContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row {
                                    ToggleSettingsAction(
                                        isShowingSettings = !isShowingSettingMenu,
                                        onCallAction = {
                                            isShowingSettingMenu = !isShowingSettingMenu
                                        },
                                    )
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                    if (call.hasCapability(OwnCapability.StartRecordCall) || call.hasCapability(
                                            OwnCapability.StopRecordCall,
                                        )
                                    ) {
                                        ToggleAction(
                                            progress = showEndRecordingDialog,
                                            isActionActive = !isRecording,
                                            iconOnOff = Pair(
                                                Icons.Default.RadioButtonChecked,
                                                Icons.Default.RadioButtonChecked,
                                            ),
                                            onAction = {
                                                GlobalScope.launch {
                                                    if (isRecording) {
                                                        showEndRecordingDialog = true
                                                    } else {
                                                        call.startRecording()
                                                    }
                                                }
                                            },
                                        )
                                        Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                    }
                                    ToggleCameraAction(
                                        isCameraEnabled = isCameraEnabled,
                                        onCallAction = { call.camera.setEnabled(it.isEnabled) },
                                    )
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                    ToggleMicrophoneAction(
                                        isMicrophoneEnabled = isMicrophoneEnabled,
                                        onCallAction = {
                                            call.microphone.setEnabled(
                                                it.isEnabled,
                                            )
                                        },
                                    )
                                    Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))
                                }
                                Row {
                                    StreamBadgeBox(
                                        text = participantsSize.size.toString(),
                                    ) {
                                        GenericAction(icon = Icons.Default.People) {
                                            showParticipants = !showParticipants
                                        }
                                    }
                                    ChatDialogAction(
                                        messageCount = unreadCount,
                                        onCallAction = { scope.launch { chatState.show() } },
                                    )
                                }
                            }
                        },
                        videoRenderer = { modifier, call, participant, style ->
                            ParticipantVideo(
                                modifier = modifier,
                                call = call,
                                participant = participant,
                                style = style,
                                reactionContent = {
                                    CustomReactionContent(
                                        participant = participant,
                                        style = style.copy(
                                            reactionPosition = Alignment.TopCenter,
                                            reactionDuration = 5000,
                                        ),
                                    )
                                },
                            )
                        },
                        floatingVideoRenderer = { _, _ ->
                            FloatingParticipantVideo(
                                call = call,
                                participant = me!!,
                                parentBounds = IntSize(
                                    this@BoxWithConstraints.constraints.maxWidth,
                                    this@BoxWithConstraints.constraints.maxHeight,
                                ),
                                videoRenderer = { participant ->
                                    ParticipantVideo(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(VideoTheme.shapes.dialog),
                                        call = call,
                                        participant = participant,
                                        reactionContent = {
                                            CustomReactionContent(
                                                participant = participant,
                                                style = RegularVideoRendererStyle().copy(
                                                    isShowingConnectionQualityIndicator = false,
                                                    reactionPosition = Alignment.TopCenter,
                                                    reactionDuration = 5000,
                                                ),
                                            )
                                        },
                                    )
                                },
                            )
                        },
                        videoOverlayContent = {
                            Crossfade(
                                modifier = Modifier.align(Alignment.BottomStart),
                                targetState = messagesVisibility,
                                label = "chat_overlay",
                            ) { visibility ->
                                if (visibility) {
                                    ChatOverly(messages = messages)
                                }
                            }
                        },
                    )
                }
            },
            updateUnreadCount = { unreadCount = it },
            onNewMessages = { updatedMessages ->
                messages.clear()
                messages += updatedMessages
                messageScope.launch {
                    messagesVisibility = true
                    delay(10000L)
                    messagesVisibility = false
                }
            },
            onDismissed = {
                if (chatState.currentValue == ModalBottomSheetValue.Expanded) {
                    scope.launch { chatState.hide() }
                } else {
                    onCallDisconnected.invoke()
                }
            },
        )

        if (participantsSize.size == 1 && !chatState.isVisible) {
            val context = LocalContext.current
            val clipboardManager = remember(context) {
                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            }
            val env = AppConfig.currentEnvironment.collectAsStateWithLifecycle()
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -VideoTheme.dimens.generic3xl.toPx().toInt()),
            ) {
                ShareCallWithOthers(
                    modifier = Modifier.fillMaxWidth(),
                    call = call,
                    clipboardManager = clipboardManager,
                    env = env,
                    context = context,
                )
            }
        }

        if (speakingWhileMuted) {
            SpeakingWhileMuted()
        }

        if (isShowingSettingMenu) {
            SettingsMenu(
                call = call,
                showDebugOptions = showDebugOptions,
                isBackgroundBlurEnabled = isBackgroundBlurEnabled,
                onDisplayAvailableDevice = { isShowingAvailableDeviceMenu = true },
                onDismissed = { isShowingSettingMenu = false },
                onShowReactionsMenu = { isShowingReactionsMenu = true },
                onToggleBackgroundBlur = {
                    isBackgroundBlurEnabled = !isBackgroundBlurEnabled
                    isShowingSettingMenu = false
                },
            ) {
                isShowingStats = true
                isShowingSettingMenu = false
            }
        }

        if (isShowingReactionsMenu) {
            ReactionsMenu(
                call = call,
                reactionMapper = VideoTheme.reactionMapper,
                onDismiss = { isShowingReactionsMenu = false },
            )
        }

        if (isShowingLayoutChooseMenu) {
            LayoutChooser(
                onLayoutChoice = {
                    layout = it
                    isShowingLayoutChooseMenu = false
                },
                current = layout,
                onDismiss = { isShowingLayoutChooseMenu = false },
            )
        }

        if (isShowingStats) {
            CallStatsDialog(call) { isShowingStats = false }
        }

        if (showParticipants) {
            ParticipantsDialog(call) {
                showParticipants = false
            }
        }

        if (isShowingAvailableDeviceMenu) {
            AvailableDeviceMenu(
                call = call,
                onDismissed = { isShowingAvailableDeviceMenu = false },
            )
        }

        // TODO: AAP, move recording and actions in separate composables.
        if (isRecording && !showRecordingWarning) {
            StreamDialogPositiveNegative(
                title = "This call is being recorded",
                contentText = "By staying in the call you’re consenting to being recorded.",
                positiveButton = Triple(
                    "Continue",
                    VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
                ) {
                    showRecordingWarning = true
                    acceptedCallRecording = true
                },
                negativeButton = Triple(
                    "Leave",
                    VideoTheme.styles.buttonStyles.tetriaryButtonStyle(),
                ) {
                    showRecordingWarning = false
                    acceptedCallRecording = false
                    call.leave()
                },
            )
        }
        if (showEndRecordingDialog) {
            StreamDialogPositiveNegative(
                title = "End recording",
                contentText = "Are you sure you want to end the recording?",
                positiveButton = Triple(
                    "End",
                    VideoTheme.styles.buttonStyles.alertButtonStyle(),
                ) {
                    GlobalScope.launch {
                        call.stopRecording()
                    }
                    showEndRecordingDialog = false
                },
                negativeButton = Triple(
                    "Cancel",
                    VideoTheme.styles.buttonStyles.tetriaryButtonStyle(),
                ) {
                    showEndRecordingDialog = false
                },
            )
        }
    }
}

@Composable
private fun SpeakingWhileMuted() {
    Snackbar {
        Text(text = "You're talking while muting the microphone!")
    }
}

@Preview
@Composable
private fun CallScreenPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallScreen(call = previewCall)
    }
}
