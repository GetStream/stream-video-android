package io.getstream.video.android.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ChatDialogAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.SettingsAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InCallScreenContent(
    call: Call,
    onLeaveCall: () -> Unit = {},
    showDebugOptions: Boolean = false,
) {

    val isCameraEnabled by call.camera.isEnabled.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()
    val speakingWhileMuted by call.state.speakingWhileMuted.collectAsState()
    var isShowingSettingMenu by remember { mutableStateOf(false) }
    var isShowingAvailableDeviceMenu by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableStateOf(0) }

    val chatState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    ChatDialog(
        state = chatState,
        call = call,
        content = {
            CallContent(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                call = call,
                enableInPictureInPicture = true,
                onBackPressed = {
                    if (chatState.currentValue == ModalBottomSheetValue.HalfExpanded) {
                        scope.launch { chatState.hide() }
                    } else {
                        onLeaveCall.invoke()
                    }
                },
                controlsContent = {
                    ControlActions(
                        call = call,
                        actions = listOf(
                            {
                                SettingsAction(
                                    modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                    onCallAction = { isShowingSettingMenu = true }
                                )
                            },
                            {
                                Box(modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize)) {
                                    ChatDialogAction(
                                        modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                        onCallAction = { scope.launch { chatState.show() } }
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
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            {
                                ToggleCameraAction(
                                    modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                    isCameraEnabled = isCameraEnabled,
                                    onCallAction = { call.camera.setEnabled(it.isEnabled) }
                                )
                            },
                            {
                                ToggleMicrophoneAction(
                                    modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                    isMicrophoneEnabled = isMicrophoneEnabled,
                                    onCallAction = { call.microphone.setEnabled(it.isEnabled) }
                                )
                            },
                            {
                                FlipCameraAction(
                                    modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                    onCallAction = { call.camera.flip() }
                                )
                            },
                            {
                                CancelCallAction(
                                    modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                    onCallAction = { onLeaveCall.invoke() }
                                )
                            },
                        ),
                    )
                }
            )
        },
        updateUnreadCount = { unreadCount = it },
        onDismissed = { scope.launch { chatState.hide() } }
    )

    if (speakingWhileMuted) {
        SpeakingWhileMuted()
    }

    if (isShowingSettingMenu) {
        SettingsMenu(
            call = call,
            showDebugOptions = showDebugOptions,
            onDisplayAvailableDevice = { isShowingAvailableDeviceMenu = true },
            onDismissed = { isShowingSettingMenu = false }
        )
    }

    if (isShowingAvailableDeviceMenu) {
        AvailableDeviceMenu(
            call = call,
            onDismissed = { isShowingAvailableDeviceMenu = false }
        )
    }
}

@Composable
private fun SpeakingWhileMuted() {
    Snackbar {
        Text(text = "You're talking while muting the microphone!")
    }
}
