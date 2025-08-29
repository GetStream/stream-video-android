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

package io.getstream.video.android.compose.ui.components.call.activecall

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.compose.permission.rememberCallPermissionsState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.fillCircle
import io.getstream.video.android.compose.ui.components.call.activecall.internal.DefaultPermissionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.MicSelectorDropDown
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallControlsV2
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.telecom.TelecomPermissions
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

/**
 * Audio call content represents the UI of an active audio only call.
 *
 * @param call The call includes states and will be rendered with participants.
 * @param modifier Modifier for styling.
 * @param isMicrophoneEnabled weather or not the microphone icon will be enabled or not
 * @param permissions the permissions required for the call to work (e.g. manifest.RECORD_AUDIO)
 * @param onBackPressed Handler when the user taps on the back button.
 * @param permissions Android permissions that should be required to render a video call properly.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param controlsContent Content is shown that allows users to trigger different actions to control a joined call.
 * @param durationPlaceholder Content (text) shown while the duration is not available yet
 * @param isShowingHeader if true, header content is shown
 * @param headerContent override the header content
 * @param detailsContent override the details content (middle part of the screen)
 */
@Deprecated(
    message = "AudioCallContent is deprecated. Use the new AudioOnlyCallContent instead.",
    replaceWith = ReplaceWith("AudioOnlyCallContent"),
)
@Composable
public fun AudioCallContent(
    modifier: Modifier = Modifier,
    call: Call,
    isMicrophoneEnabled: Boolean,
    permissions: VideoPermissionsState = rememberCallPermissionsState(
        call = call,
        permissions = getPermissions(),
    ),
    onCallAction: (CallAction) -> Unit = { action: CallAction ->
        DefaultOnCallActionHandler.onCallAction(call, action)
    },
    durationPlaceholder: String = "",
    isShowingHeader: Boolean = true,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    detailsContent: (
        @Composable ColumnScope.(
            participants: List<MemberState>,
            topPadding: Dp,
        ) -> Unit
    )? = null,
    controlsContent: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit = {},
) {
    val duration by call.state.duration.collectAsStateWithLifecycle()
    val durationText = duration?.toString() ?: durationPlaceholder

    DefaultPermissionHandler(videoPermission = permissions)

    OutgoingCallContent(
        modifier = modifier,
        isShowingHeader = isShowingHeader,
        headerContent = headerContent,
        call = call,
        isVideoType = false,
        detailsContent = detailsContent ?: { members, topPadding ->
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                ParticipantAvatars(members = members)
                Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))
                ParticipantInformation(
                    isVideoType = false,
                    callStatus = CallStatus.Calling(durationText),
                    members = members,
                )
            }
        },
        onBackPressed = onBackPressed,
        controlsContent = controlsContent ?: {
            val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()
            val selectedMicroPhoneDevice by call.microphone.selectedDevice.collectAsStateWithLifecycle()
            val audioDeviceUiStateList: List<AudioDeviceUiState> = availableDevices.map {
                val icon = when (it) {
                    is StreamAudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
                    is StreamAudioDevice.Earpiece -> Icons.Default.Headphones
                    is StreamAudioDevice.Speakerphone -> Icons.Default.SpeakerPhone
                    is StreamAudioDevice.WiredHeadset -> Icons.Default.HeadsetMic
                }
                AudioDeviceUiState(
                    it,
                    it.name,
                    icon,
                    it.name == selectedMicroPhoneDevice?.name,
                )
            }

            if (selectedMicroPhoneDevice != null) {
                OutgoingCallControlsV2(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(VideoTheme.dimens.spacingM),
                    isVideoCall = false,
                    isCameraEnabled = false,
                    isMicrophoneEnabled = isMicrophoneEnabled,
                    audioDeviceUiStateList = audioDeviceUiStateList,
                    call = call,
                    onCallAction = onCallAction,
                )
            }
        },
    )
}

@Composable
private fun getPermissions(): List<String> {
    val context = LocalContext.current
    val permissionsList = mutableListOf<String>()
    val telecomPermissions = TelecomPermissions()

    with(telecomPermissions) {
        val optedForTelecom = StreamVideo.instanceOrNull()?.state?.optedForTelecom() == true
        if (optedForTelecom && supportsTelecom(context)) {
            permissionsList.addAll(getRequiredPermissionsList())
        }
    }

    permissionsList.addAll(
        mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
        ),
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionsList.addAll(
            mutableListOf(
                android.Manifest.permission.BLUETOOTH_CONNECT, // This is new addition to addition to audio call content, it should be added only when services are correctly configured
            ),
        )
    }
    return permissionsList
}

/**
 * Represents the UI for an active audio-only call. By default, it shows the current participants that are in the call.
 *
 * @param modifier Modifier for styling.
 * @param call The call to be rendered.
 * @param isMicrophoneEnabled Weather or not the microphone icon will show the mic as enabled or not.
 * @param permissions Android permissions that should be requested.
 * @param isShowingHeader If true, header content is shown.
 * @param headerContent Content that overrides the header.
 * @param durationPlaceholder Content (text) shown while the duration is not available.
 * @param detailsContent Content that overrides the details (the middle part of the screen).
 * @param controlsContent Content that allows users to trigger call actions. See [CallAction].
 * @param onCallAction Handler used when the user triggers a [CallAction].
 * @param onBackPressed Handler used when the user taps on the back button.
 */
@Composable
public fun AudioOnlyCallContent(
    modifier: Modifier = Modifier,
    call: Call,
    isMicrophoneEnabled: Boolean,
    permissions: VideoPermissionsState = rememberCallPermissionsState(
        call = call,
        permissions = getPermissions(),
    ),
    isShowingHeader: Boolean = true,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    durationPlaceholder: String = "",
    detailsContent: (
        @Composable ColumnScope.(
            remoteParticipants: List<ParticipantState>,
            topPadding: Dp,
        ) -> Unit
    )? = null,
    controlsContent: (@Composable BoxScope.() -> Unit)? = null,
    onCallAction: (CallAction) -> Unit = { action: CallAction ->
        DefaultOnCallActionHandler.onCallAction(call, action)
    },
    onBackPressed: () -> Unit = {},
) {
    val remoteParticipants by call.state.remoteParticipants.collectAsStateWithLifecycle()
    val duration by call.state.duration.collectAsStateWithLifecycle()
    val durationText = duration?.toString() ?: durationPlaceholder

    DefaultPermissionHandler(videoPermission = permissions)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = VideoTheme.colors.baseSheetTertiary),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
        ) {
            if (isShowingHeader) headerContent?.invoke(this)

            detailsContent?.invoke(this, remoteParticipants, VideoTheme.dimens.spacingM)
                ?: AudioOnlyCallDetails(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    participants = remoteParticipants,
                    duration = durationText,
                )
        }
        val selectedMicroPhoneDevice by call.microphone.selectedDevice.collectAsStateWithLifecycle()
        val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()
        val audioDeviceUiStateList: List<AudioDeviceUiState> = availableDevices.map {
            val icon = when (it) {
                is StreamAudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
                is StreamAudioDevice.Earpiece -> Icons.Default.Headphones
                is StreamAudioDevice.Speakerphone -> Icons.Default.SpeakerPhone
                is StreamAudioDevice.WiredHeadset -> Icons.Default.HeadsetMic
            }
            AudioDeviceUiState(
                it,
                it.name,
                icon,
                it.name == selectedMicroPhoneDevice?.name,
            )
        }

        controlsContent?.invoke(this) ?: AudioOnlyCallControlsV2(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.genericXxl),
            isMicrophoneEnabled = isMicrophoneEnabled,
            audioDeviceUiStateList = audioDeviceUiStateList,
            call = call,
            onCallAction = onCallAction,
        )
    }
}

/**
 * Component that displays details for an active audio-only call.
 *
 * @param modifier Modifier for styling.
 * @param duration The current duration of the call.
 * @param participants A list of current call participants to be displayed.
 */
@Composable
public fun AudioOnlyCallDetails(
    modifier: Modifier = Modifier,
    duration: String,
    participants: List<ParticipantState>,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        ParticipantAvatars(participants = participants)

        Spacer(modifier = Modifier.height(32.dp))

        ParticipantInformation(
            isVideoType = false,
            callStatus = CallStatus.Calling(duration),
            participants = participants,
        )
    }
}

/**
 * Component that displays the call controls for an active audio-only call.
 *
 * @param modifier Modifier for styling.
 * @param isMicrophoneEnabled Weather or not the microphone icon will show the mic as enabled or not.
 * @param onCallAction Handler used when the user triggers a [CallAction].
 */
@Deprecated("Use the new AudioOnlyCallControlsV2 instead.", level = DeprecationLevel.ERROR)
@Composable
public fun AudioOnlyCallControls(
    modifier: Modifier,
    isMicrophoneEnabled: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier.testTag("Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled"),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(1.5f),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(1.5f),
        )

        LeaveCallAction(
            modifier = Modifier.testTag("Stream_HangUpButton"),
            onCallAction = onCallAction,
            style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle().fillCircle(1.5f),
        )
    }
}

@Composable
public fun AudioOnlyCallControlsV2(
    modifier: Modifier,
    isMicrophoneEnabled: Boolean,
    call: Call,
    audioDeviceUiStateList: List<AudioDeviceUiState>,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier.testTag("Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled"),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(1.5f),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(1.5f),
        )

        if (audioDeviceUiStateList.isNotEmpty()) {
            MicSelectorDropDown(call, audioDeviceUiStateList)
        }

        LeaveCallAction(
            modifier = Modifier.testTag("Stream_HangUpButton"),
            onCallAction = onCallAction,
            style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle().fillCircle(1.5f),
        )
    }
}

@Preview
@Composable
private fun AudioCallContentPreview() {
    val context = LocalContext.current
    StreamPreviewDataUtils.initializeStreamVideo(context)
    VideoTheme {
        AudioCallContent(
            call = previewCall,
            isMicrophoneEnabled = false,
            durationPlaceholder = "11:45",
        )
    }
}

@Preview
@Composable
private fun AudioOnlyCallContentPreview() {
    val context = LocalContext.current
    StreamPreviewDataUtils.initializeStreamVideo(context)
    VideoTheme {
        AudioOnlyCallContent(
            call = previewCall,
            isMicrophoneEnabled = false,
        )
    }
}

public data class AudioDeviceUiState(
    val streamAudioDevice: StreamAudioDevice,
    val text: String,
    val icon: ImageVector, // Assuming it's a drawable resource ID
    val highlight: Boolean,
)
