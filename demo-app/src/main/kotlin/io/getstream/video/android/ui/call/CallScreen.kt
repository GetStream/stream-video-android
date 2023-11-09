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

@file:OptIn(ExperimentalMaterialApi::class)

package io.getstream.video.android.ui.call

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.chat.android.ui.common.state.messages.list.MessageItemState
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ChatDialogAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.SettingsAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.renderer.LayoutType
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.call.state.ChooseLayout
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CallScreen(
    call: Call,
    showDebugOptions: Boolean = false,
    onCallDisconnected: () -> Unit = {},
    onUserLeaveCall: () -> Unit = {},
) {
    val context = LocalContext.current
    val isCameraEnabled by call.camera.isEnabled.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()
    val speakingWhileMuted by call.state.speakingWhileMuted.collectAsState()
    var isShowingSettingMenu by remember { mutableStateOf(false) }
    var isShowingLayoutChooseMenu by remember { mutableStateOf(false) }
    var isShowingReactionsMenu by remember { mutableStateOf(false) }
    var isShowingAvailableDeviceMenu by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf(LayoutType.DYNAMIC) }
    var unreadCount by remember { mutableIntStateOf(0) }
    val chatState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val messages: MutableList<MessageItemState> = remember { mutableStateListOf() }
    var messagesVisibility by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messageScope = rememberCoroutineScope()

    val callState by call.state.connection.collectAsState()

    LaunchedEffect(key1 = callState) {
        if (callState == RealtimeConnection.Disconnected) {
            onCallDisconnected.invoke()
        } else if (callState is RealtimeConnection.Failed) {
            Toast.makeText(
                context,
                "Call connection failed (${(callState as RealtimeConnection.Failed).error}",
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
                Box(modifier = Modifier.fillMaxSize()) {
                    CallContent(
                        modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                        call = call,
                        layout = layout,
                        enableInPictureInPicture = true,
                        enableDiagnostics = BuildConfig.DEBUG,
                        onCallAction = {
                            when (it) {
                                ChooseLayout -> isShowingLayoutChooseMenu = true
                                else -> DefaultOnCallActionHandler.onCallAction(
                                    call,
                                    it,
                                )
                            }
                        },
                        onBackPressed = {
                            if (chatState.currentValue == ModalBottomSheetValue.Expanded) {
                                scope.launch { chatState.hide() }
                            } else {
                                onUserLeaveCall.invoke()
                            }
                        },
                        controlsContent = {
                            ControlActions(
                                call = call,
                                actions = listOf(
                                    {
                                        SettingsAction(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.controlActionsButtonSize,
                                            ),
                                            onCallAction = { isShowingSettingMenu = true },
                                        )
                                    },
                                    {
                                        Box(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.controlActionsButtonSize,
                                            ),
                                        ) {
                                            ChatDialogAction(
                                                modifier = Modifier.size(
                                                    VideoTheme.dimens.controlActionsButtonSize,
                                                ),
                                                onCallAction = { scope.launch { chatState.show() } },
                                            )

                                            if (unreadCount > 0) {
                                                Badge(
                                                    modifier = Modifier.align(Alignment.TopEnd),
                                                    backgroundColor = VideoTheme.colors.errorAccent,
                                                    contentColor = VideoTheme.colors.errorAccent,
                                                ) {
                                                    Text(
                                                        text = unreadCount.toString(),
                                                        color = VideoTheme.colors.textHighEmphasis,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    {
                                        ToggleCameraAction(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.controlActionsButtonSize,
                                            ),
                                            isCameraEnabled = isCameraEnabled,
                                            onCallAction = { call.camera.setEnabled(it.isEnabled) },
                                        )
                                    },
                                    {
                                        ToggleMicrophoneAction(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.controlActionsButtonSize,
                                            ),
                                            isMicrophoneEnabled = isMicrophoneEnabled,
                                            onCallAction = {
                                                call.microphone.setEnabled(
                                                    it.isEnabled,
                                                )
                                            },
                                        )
                                    },
                                    {
                                        FlipCameraAction(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.controlActionsButtonSize,
                                            ),
                                            onCallAction = { call.camera.flip() },
                                        )
                                    },
                                    {
                                        CancelCallAction(
                                            modifier = Modifier.size(
                                                VideoTheme.dimens.controlActionsButtonSize,
                                            ),
                                            onCallAction = { onUserLeaveCall.invoke() },
                                        )
                                    },
                                ),
                            )
                        },
                        videoOverlayContent = {
                            Crossfade(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(
                                        horizontal = VideoTheme.dimens.participantLabelPadding,
                                        vertical = VideoTheme.dimens.participantLabelHeight + 8.dp,
                                    ),
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

        if (speakingWhileMuted) {
            SpeakingWhileMuted()
        }

        if (isShowingSettingMenu) {
            SettingsMenu(
                call = call,
                showDebugOptions = showDebugOptions,
                onDisplayAvailableDevice = { isShowingAvailableDeviceMenu = true },
                onDismissed = { isShowingSettingMenu = false },
                onShowReactionsMenu = { isShowingReactionsMenu = true },
            )
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

        if (isShowingAvailableDeviceMenu) {
            AvailableDeviceMenu(
                call = call,
                onDismissed = { isShowingAvailableDeviceMenu = false },
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
