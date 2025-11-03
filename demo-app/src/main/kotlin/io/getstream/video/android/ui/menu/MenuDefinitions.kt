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

import android.media.MediaCodecInfo
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionUiState
import io.getstream.video.android.ui.menu.base.ActionMenuItem
import io.getstream.video.android.ui.menu.base.DynamicSubMenuItem
import io.getstream.video.android.ui.menu.base.MenuItem
import io.getstream.video.android.ui.menu.base.SubMenuItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Defines the default Stream menu for the demo app.
 * @param availableDevices ❗️Deprecated: This parameter is deprecated and will removed in future version, it is replaced with audioDeviceUiStateList
 */
fun defaultStreamMenu(
    showDebugOptions: Boolean = false,
    noiseCancellationFeatureEnabled: Boolean = false,
    noiseCancellationEnabled: Boolean = false,
    codecList: List<MediaCodecInfo>,
    onCodecSelected: (MediaCodecInfo) -> Unit,
    onShowCallStats: () -> Unit,
    onToggleAudioFilterClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onRestartPublisherIceClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
    onShowFeedback: () -> Unit,
    onNoiseCancellation: () -> Unit,
    selectedIncomingVideoResolution: PreferredVideoResolution?,
    onSelectIncomingVideoResolution: (PreferredVideoResolution?) -> Unit,
    isIncomingVideoEnabled: Boolean,
    onToggleIncomingVideoEnabled: (Boolean) -> Unit,
    onDeviceSelected: (StreamAudioDevice) -> Unit,
    onSfuRejoinClick: () -> Unit,
    onSfuFastReconnectClick: () -> Unit,
    onSelectScaleType: (VideoScalingType) -> Unit,
    availableDevices: List<StreamAudioDevice>,
    loadRecordings: suspend () -> List<MenuItem>,
    transcriptionUiState: TranscriptionUiState,
    onToggleTranscription: suspend () -> Unit,
    loadTranscriptions: suspend () -> List<MenuItem>,
    onToggleClosedCaptions: () -> Unit = {},
    closedCaptionUiState: ClosedCaptionUiState,
    audioDeviceUiStateList: List<AudioDeviceUiState> = emptyList(),
    audioUsageUiState: AudioUsageUiState = AudioUsageVoiceCommunicationUiState,
    onToggleAudioUsage: () -> Unit = {},
) = buildList<MenuItem> {
    if (noiseCancellationFeatureEnabled) {
        add(
            ActionMenuItem(
                title = "Noise cancellation",
                icon = Icons.Default.SpatialAudioOff,
                highlight = noiseCancellationEnabled,
                action = onNoiseCancellation,
            ),
        )
    }
    when (transcriptionUiState) {
        is TranscriptionAvailableUiState, TranscriptionStoppedUiState -> {
            add(
                ActionMenuItem(
                    title = transcriptionUiState.text,
                    icon = transcriptionUiState.icon,
                    highlight = transcriptionUiState.highlight,
                    action = {
                        GlobalScope.launch {
                            onToggleTranscription.invoke()
                        }
                    },
                ),
            )
        }

        else -> {}
    }
    add(getCCActionMenu(closedCaptionUiState, onToggleClosedCaptions))
    add(
        SubMenuItem(
            title = "Choose audio device",
            icon = Icons.Default.SettingsVoice,
            items = audioDeviceUiStateList.map {
                ActionMenuItem(
                    title = it.text,
                    icon = it.icon,
                    action = { onDeviceSelected(it.streamAudioDevice) },
                    highlight = it.highlight,
                )
            },
        ),
    )
    add(
        ActionMenuItem(
            title = "Call stats",
            icon = Icons.Default.AutoGraph,
            action = onShowCallStats,
        ),
    )

    if (showDebugOptions) {
        add(
            SubMenuItem(
                title = "Debug options",
                icon = Icons.AutoMirrored.Default.ReadMore,
                items = debugSubmenu(
                    codecList,
                    onCodecSelected,
                    onToggleAudioFilterClick,
                    onRestartSubscriberIceClick,
                    onRestartPublisherIceClick,
                    onSwitchSfuClick,
                    onSfuRejoinClick,
                    onSfuFastReconnectClick,
                    onSelectScaleType,
                    loadRecordings,
                    onShowFeedback,
                    selectedIncomingVideoResolution,
                    onSelectIncomingVideoResolution,
                    isIncomingVideoEnabled,
                    onToggleIncomingVideoEnabled,
                    loadTranscriptions,
                    audioUsageUiState,
                    onToggleAudioUsage,
                ),
            ),
        )
    }
}

fun getCCActionMenu(
    closedCaptionUiState: ClosedCaptionUiState,
    onToggleClosedCaptions: () -> Unit,
): ActionMenuItem {
    return when (closedCaptionUiState) {
        is ClosedCaptionUiState.Available -> {
            ActionMenuItem(
                title = "Start Closed Caption",
                icon = Icons.Default.ClosedCaptionOff,
                action = onToggleClosedCaptions,
            )
        }

        is ClosedCaptionUiState.Running -> {
            ActionMenuItem(
                title = "Stop Closed Caption",
                icon = Icons.Default.ClosedCaption,
                action = onToggleClosedCaptions,
            )
        }

        is ClosedCaptionUiState.UnAvailable -> {
            ActionMenuItem(
                title = "Closed Caption are unavailable",
                icon = Icons.Default.ClosedCaptionDisabled,
                action = { },
            )
        }
    }
}

/**
 * Lists the available codecs for this device as list of [MenuItem]
 */
fun codecMenu(codecList: List<MediaCodecInfo>, onCodecSelected: (MediaCodecInfo) -> Unit) =
    codecList.map {
        val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            it.isHardwareAccelerated
        } else {
            false
        }
        ActionMenuItem(
            title = it.name,
            icon = Icons.Default.VideoFile,
            highlight = isHw,
            action = { onCodecSelected(it) },
        )
    }

fun reconnectMenu(
    onRestartPublisherIceClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
    onSfuRejoinClick: () -> Unit,
    onSfuFastReconnectClick: () -> Unit,
) = listOf(
    ActionMenuItem(
        title = "Publisher - ICE restart",
        icon = Icons.Default.SettingsBackupRestore,
        action = onRestartPublisherIceClick,
    ),
    ActionMenuItem(
        title = "Subscriber - ICE restart",
        icon = Icons.Default.SettingsBackupRestore,
        action = onRestartSubscriberIceClick,
    ),
    ActionMenuItem(
        title = "Reconnect SFU - migrate",
        icon = Icons.Default.SwitchLeft,
        action = onSwitchSfuClick,
    ),
    ActionMenuItem(
        title = "Reconnect SFU - rejoin",
        icon = Icons.Default.Replay,
        action = onSfuRejoinClick,
    ),
    ActionMenuItem(
        title = "Reconnect SFU - fast",
        icon = Icons.Default.RestartAlt,
        action = onSfuFastReconnectClick,
    ),
)

fun scaleTypeMenu(onSelectScaleType: (VideoScalingType) -> Unit): List<MenuItem> = listOf(
    ActionMenuItem(
        title = "Scale FIT",
        icon = Icons.Default.CropFree,
        action = { onSelectScaleType(VideoScalingType.SCALE_ASPECT_FIT) },
    ),
    ActionMenuItem(
        title = "Scale FILL",
        icon = Icons.Default.Crop,
        action = { onSelectScaleType(VideoScalingType.SCALE_ASPECT_FILL) },
    ),
    ActionMenuItem(
        title = "Scale BALANCED",
        icon = Icons.Default.Balance,
        action = { onSelectScaleType(VideoScalingType.SCALE_ASPECT_BALANCED) },
    ),
)

/**
 * Optionally defines the debug sub-menu of the demo app.
 */
fun debugSubmenu(
    codecList: List<MediaCodecInfo>,
    onCodecSelected: (MediaCodecInfo) -> Unit,
    onToggleAudioFilterClick: () -> Unit,
    onRestartPublisherIceClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
    onSfuRejoinClick: () -> Unit,
    onSfuFastReconnectClick: () -> Unit,
    onSelectScaleType: (VideoScalingType) -> Unit,
    loadRecordings: suspend () -> List<MenuItem>,
    onShowFeedback: () -> Unit,
    selectedIncomingVideoResolution: PreferredVideoResolution?,
    onSelectIncomingVideoResolution: (PreferredVideoResolution?) -> Unit,
    isIncomingVideoEnabled: Boolean,
    onToggleIncomingVideoEnabled: (Boolean) -> Unit,
    loadTranscriptions: suspend () -> List<MenuItem>,
    audioUsageUiState: AudioUsageUiState,
    onToggleAudioUsage: () -> Unit,
) = listOf(
    DynamicSubMenuItem(
        title = "List Transcriptions",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        itemsLoader = loadTranscriptions,
    ),
    SubMenuItem(
        title = "Incoming video settings",
        icon = Icons.Default.VideoSettings,
        items = listOf(
            ActionMenuItem(
                title = "Auto Quality",
                icon = Icons.Default.AspectRatio,
                highlight = selectedIncomingVideoResolution == null,
                action = { onSelectIncomingVideoResolution(null) },
            ),
            ActionMenuItem(
                title = "4K 2160p",
                icon = Icons.Default.AspectRatio,
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(3840, 2160),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(3840, 2160))
                },
            ),
            ActionMenuItem(
                title = "Full HD 1080p",
                icon = Icons.Default.AspectRatio,
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(1920, 1080),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(1920, 1080))
                },
            ),
            ActionMenuItem(
                title = "HD 720p",
                icon = Icons.Default.AspectRatio,
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(1280, 720),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(1280, 720))
                },
            ),
            ActionMenuItem(
                title = "SD 480p",
                icon = Icons.Default.AspectRatio,
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(640, 480),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(640, 480))
                },
            ),
            ActionMenuItem(
                title = "Data Saver 144p",
                icon = Icons.Default.AspectRatio,
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(256, 144),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(256, 144))
                },
            ),
            ActionMenuItem(
                title = if (isIncomingVideoEnabled) "Disable incoming video" else "Enable incoming video",
                icon = if (isIncomingVideoEnabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                action = { onToggleIncomingVideoEnabled(!isIncomingVideoEnabled) },
            ),
        ),
    ),
    SubMenuItem(
        title = "Scale type",
        icon = Icons.Default.AspectRatio,
        items = scaleTypeMenu(
            onSelectScaleType,
        ),
    ),
    SubMenuItem(
        title = "Available video codecs",
        icon = Icons.Default.VideoSettings,
        items = codecMenu(codecList, onCodecSelected),
    ),
    ActionMenuItem(
        title = "Toggle audio filter",
        icon = Icons.Default.Audiotrack,
        action = onToggleAudioFilterClick,
    ),
    ActionMenuItem(
        title = audioUsageUiState.text,
        icon = audioUsageUiState.icon,
        highlight = audioUsageUiState.highlight,
        action = onToggleAudioUsage,
    ),
    ActionMenuItem(
        title = "Start/stop recording",
        icon = Icons.Default.RadioButtonChecked,
        action = {
//                scope.launch {
//                    if (isRecording) {
//                        showEndRecordingDialog = true
//                    } else {
//                        call.startRecording()
//                    }
//                }
        },
    ),
    DynamicSubMenuItem(
        title = "Recordings",
        icon = Icons.Default.VideoLibrary,
        itemsLoader = loadRecordings,
    ),
    ActionMenuItem(
        title = "Feedback",
        icon = Icons.Default.Feedback,
        action = onShowFeedback,
    ),
    SubMenuItem(
        title = "Reconnect V2",
        icon = Icons.Default.Replay,
        items = reconnectMenu(
            onRestartPublisherIceClick,
            onRestartSubscriberIceClick,
            onSwitchSfuClick,
            onSfuRejoinClick,
            onSfuFastReconnectClick,
        ),
    ),
)
