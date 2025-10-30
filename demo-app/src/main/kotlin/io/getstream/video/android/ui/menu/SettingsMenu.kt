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

package io.getstream.video.android.ui.menu

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.media.AudioAttributes
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.ui.call.ReactionsMenu
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionUiState
import io.getstream.video.android.ui.menu.base.ActionMenuItem
import io.getstream.video.android.ui.menu.base.DynamicMenu
import io.getstream.video.android.ui.menu.base.MenuItem
import io.getstream.video.android.ui.menu.transcriptions.TranscriptionUiStateManager
import io.getstream.video.android.util.filters.SampleAudioFilter
import java.nio.ByteBuffer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun SettingsMenu(
    call: Call,
    selectedVideoFilter: Int,
    showDebugOptions: Boolean,
    noiseCancellationFeatureEnabled: Boolean,
    noiseCancellationEnabled: Boolean,
    onDismissed: () -> Unit,
    onSelectVideoFilter: (Int) -> Unit,
    onShowFeedback: () -> Unit,
    onNoiseCancellation: () -> Unit,
    selectedIncomingVideoResolution: PreferredVideoResolution?,
    onSelectIncomingVideoResolution: (PreferredVideoResolution?) -> Unit,
    isIncomingVideoEnabled: Boolean,
    onToggleIncomingVideoVisibility: (Boolean) -> Unit,
    onShowCallStats: () -> Unit,
    onSelectScaleType: (VideoScalingType) -> Unit,
    closedCaptionUiState: ClosedCaptionUiState,
    onClosedCaptionsToggle: () -> Unit,
) {
    val context = LocalContext.current
    val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()
    val currentAudioUsage by call.speaker.audioUsage.collectAsStateWithLifecycle()

    val audioUsageUiState = remember(currentAudioUsage) {
        getAudioUsageUiState(currentAudioUsage)
    }

    val onToggleAudioUsage: suspend () -> Unit = {
        val newAudioUsage = when (audioUsageUiState) {
            AudioUsageMediaUiState -> AudioAttributes.USAGE_VOICE_COMMUNICATION
            AudioUsageVoiceCommunicationUiState -> AudioAttributes.USAGE_MEDIA
        }
        call.speaker.setAudioUsage(newAudioUsage)
    }

    val onToggleAudioFilterClick: () -> Unit = {
        if (call.audioFilter == null) {
            call.audioFilter = object : InputAudioFilter {
                override fun applyFilter(
                    audioFormat: Int,
                    channelCount: Int,
                    sampleRate: Int,
                    sampleData: ByteBuffer,
                ) {
                    SampleAudioFilter.toRoboticVoice(sampleData, channelCount, 0.8f)
                }
            }
        } else {
            call.audioFilter = null
        }
        onDismissed()
    }

    val onRestartSubscriberIceClick: () -> Unit = {
        call.debug.restartSubscriberIce()
        onDismissed.invoke()
        Toast.makeText(context, "Restart Subscriber Ice", Toast.LENGTH_SHORT).show()
    }

    val onRestartPublisherIceClick: () -> Unit = {
        call.debug.restartPublisherIce()
        onDismissed.invoke()
        Toast.makeText(context, "Restart Publisher Ice", Toast.LENGTH_SHORT).show()
    }

    val onSfuRejoinClick: () -> Unit = {
        call.debug.rejoin()
        onDismissed.invoke()
        Toast.makeText(context, "Killing SFU WS. Should trigger reconnect...", Toast.LENGTH_SHORT)
            .show()
    }

    val onSwitchSfuClick: () -> Unit = {
        call.debug.migrate()
        onDismissed.invoke()
        Toast.makeText(context, "Switch sfu", Toast.LENGTH_SHORT).show()
    }

    val onSfuFastReconnectClick: () -> Unit = {
        call.debug.fastReconnect()
        onDismissed.invoke()
        Toast.makeText(context, "Fast Reconnect SFU", Toast.LENGTH_SHORT).show()
    }

    val codecInfos = remember {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter {
            it.name.contains("encoder") && it.supportedTypes.firstOrNull {
                it.contains("video")
            } != null
        }
    }

    val onLoadRecordings: suspend () -> List<MenuItem> = storagePermissionAndroidBellow10 {
        when (it) {
            is PermissionStatus.Granted -> {
                {
                    call.listRecordings().getOrNull()?.recordings?.map {
                        ActionMenuItem(
                            title = it.filename,
                            icon = Icons.Default.VideoFile,
                            action = {
                                context.downloadFile(it.url, it.filename)
                                onDismissed()
                            },
                        )
                    } ?: emptyList()
                }
            }
            is PermissionStatus.Denied -> {
                { emptyList() }
            }
        }
    }

    val isCurrentlyTranscribing by call.state.transcribing.collectAsStateWithLifecycle()
    val settings by call.state.settings.collectAsStateWithLifecycle()

    // Use the manager to determine the UI state
    val transcriptionUiStateManager =
        TranscriptionUiStateManager(isCurrentlyTranscribing, settings)
    val transcriptionUiState = transcriptionUiStateManager.getUiState()

    val onToggleTranscription: suspend () -> Unit = {
        when (transcriptionUiState) {
            TranscriptionAvailableUiState -> call.startTranscription()
            TranscriptionStoppedUiState -> call.stopTranscription()
            else -> {
                throw IllegalStateException(
                    "Toggling of transcription should not work in state: $transcriptionUiState",
                )
            }
        }
    }

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
            it.audio.name == selectedMicroPhoneDevice?.audio?.name,
        )
    }

    val onLoadTranscriptions: suspend () -> List<MenuItem> = storagePermissionAndroidBellow10 {
        when (it) {
            is PermissionStatus.Granted -> {
                {
                    call.listTranscription().getOrNull()?.transcriptions?.map {
                        ActionMenuItem(
                            title = it.filename,
                            icon = Icons.Default.VideoFile, // TODO Rahul check this later
                            action = {
                                context.downloadFile(it.url, it.filename)
                                onDismissed()
                            },
                        )
                    } ?: emptyList()
                }
            }
            is PermissionStatus.Denied -> {
                { emptyList() }
            }
        }
    }

    Popup(
        offset = IntOffset(
            0,
            -(VideoTheme.dimens.componentHeightL + VideoTheme.dimens.spacingS).toPx().toInt(),
        ),
        alignment = Alignment.BottomStart,
        onDismissRequest = { onDismissed.invoke() },
        properties = PopupProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        DynamicMenu(
            header = {
                Icon(
                    tint = Color.White,
                    imageVector = Icons.Default.Close,
                    contentDescription = Icons.Default.Close.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp).clickable {
                        onDismissed()
                    },
                )
                ReactionsMenu(
                    call = call,
                    reactionMapper = ReactionMapper.defaultReactionMapper(),
                ) {
                    onDismissed()
                }
                Spacer(Modifier.height(VideoTheme.dimens.spacingS))
                VideoFiltersMenu(
                    selectedFilterIndex = selectedVideoFilter,
                    onSelectFilter = { filterIndex ->
                        onDismissed()
                        onSelectVideoFilter(filterIndex)
                    },
                )
                Spacer(Modifier.height(VideoTheme.dimens.spacingS))
            },
            items = defaultStreamMenu(
                showDebugOptions = showDebugOptions,
                noiseCancellationFeatureEnabled = noiseCancellationFeatureEnabled,
                noiseCancellationEnabled = noiseCancellationEnabled,
                codecList = codecInfos,
                availableDevices = availableDevices,
                onDeviceSelected = {
                    call.microphone.select(it)
                    onDismissed()
                },
                onCodecSelected = {
                    onDismissed()
                },
                onShowFeedback = onShowFeedback,
                onRestartPublisherIceClick = onRestartPublisherIceClick,
                onRestartSubscriberIceClick = onRestartSubscriberIceClick,
                onToggleAudioFilterClick = onToggleAudioFilterClick,
                onSwitchSfuClick = onSwitchSfuClick,
                onShowCallStats = onShowCallStats,
                onNoiseCancellation = onNoiseCancellation,
                selectedIncomingVideoResolution = selectedIncomingVideoResolution,
                onSelectIncomingVideoResolution = { onSelectIncomingVideoResolution(it) },
                isIncomingVideoEnabled = isIncomingVideoEnabled,
                onToggleIncomingVideoEnabled = { onToggleIncomingVideoVisibility(it) },
                onSfuRejoinClick = onSfuRejoinClick,
                onSfuFastReconnectClick = onSfuFastReconnectClick,
                onSelectScaleType = onSelectScaleType,
                loadRecordings = onLoadRecordings,
                onToggleClosedCaptions = onClosedCaptionsToggle,
                closedCaptionUiState = closedCaptionUiState,
                transcriptionUiState = transcriptionUiState,
                onToggleTranscription = onToggleTranscription,
                loadTranscriptions = onLoadTranscriptions,
                audioDeviceUiStateList = audioDeviceUiStateList,
                audioUsageUiState = audioUsageUiState,
                onToggleAudioUsage = onToggleAudioUsage,
            ),
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun storagePermissionAndroidBellow10(
    permission: (PermissionStatus) -> suspend () -> List<MenuItem>,
): suspend () -> List<MenuItem> {
    // Check if the device's API level is below Android 10 (API level 29)
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val writeStoragePermissionState =
            rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        LaunchedEffect(key1 = true) {
            // Request permission
            writeStoragePermissionState.launchPermissionRequest()
        }
        permission(writeStoragePermissionState.status)
    } else {
        permission(PermissionStatus.Granted)
    }
}

private fun Context.downloadFile(url: String, title: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(title) // Title of the Download Notification
        .setDescription("Downloading") // Description of the Download Notification
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
        ) // Visibility of the download Notification
        .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
        .setAllowedOverRoaming(true) // Set if download is allowed on Roaming network
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)

    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request) // enqueue puts the download request in the queue.
}

@Preview
@Composable
private fun SettingsMenuPreview() {
    VideoTheme {
        DynamicMenu(
            items = defaultStreamMenu(
                codecList = emptyList(),
                onCodecSelected = {
                },
                onShowCallStats = { },
                onToggleAudioFilterClick = { },
                onRestartSubscriberIceClick = { },
                onRestartPublisherIceClick = { },
                onSfuRejoinClick = { },
                onSfuFastReconnectClick = {},
                onSwitchSfuClick = { },
                availableDevices = emptyList(),
                onDeviceSelected = {},
                onShowFeedback = {},
                onSelectScaleType = {},
                onNoiseCancellation = {},
                selectedIncomingVideoResolution = null,
                onSelectIncomingVideoResolution = {},
                isIncomingVideoEnabled = true,
                onToggleIncomingVideoEnabled = {},
                loadRecordings = { emptyList() },
                onToggleClosedCaptions = { },
                closedCaptionUiState = ClosedCaptionUiState.Available,
                transcriptionUiState = TranscriptionAvailableUiState,
                onToggleTranscription = {},
                loadTranscriptions = { emptyList() },
                audioDeviceUiStateList = emptyList(),
                audioUsageUiState = AudioUsageVoiceCommunicationUiState,
                onToggleAudioUsage = {},
            ),
        )
    }
}
