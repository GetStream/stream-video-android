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

package io.getstream.video.android.ui.menu

import android.media.MediaCodecInfo
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.SpatialAudioOff
import io.getstream.video.android.compose.ui.components.video.VideoScalingType
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.recording.RecordingType
import io.getstream.video.android.ui.closedcaptions.ClosedCaptionUiState
import io.getstream.video.android.ui.menu.base.ActionMenuItem
import io.getstream.video.android.ui.menu.base.DynamicSubMenuItem
import io.getstream.video.android.ui.menu.base.MenuItem
import io.getstream.video.android.ui.menu.base.SubMenuItem
import io.getstream.video.android.ui.menu.base.menuIcon
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import io.getstream.video.android.compose.R as ComposeR
import io.getstream.video.android.ui.common.R as CommonR

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
    onSimulateSfuFullClick: () -> Unit,
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
    selectedRecordingTypes: Set<RecordingType> = emptySet(),
    onSelectRecordingType: (RecordingType) -> Unit = {},
) = buildList<MenuItem> {
    if (noiseCancellationFeatureEnabled) {
        add(
            ActionMenuItem(
                title = "Noise cancellation",
                icon = menuIcon(Icons.Default.SpatialAudioOff),
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
            icon = menuIcon(ComposeR.drawable.stream_design_ic_voice_fill),
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
            icon = menuIcon(ComposeR.drawable.stream_design_ic_stats_fill),
            action = onShowCallStats,
        ),
    )

    if (showDebugOptions) {
        add(
            SubMenuItem(
                title = "Debug options",
                icon = menuIcon(ComposeR.drawable.stream_design_ic_more_horizontal),
                items = debugSubmenu(
                    codecList,
                    onCodecSelected,
                    onToggleAudioFilterClick,
                    onRestartSubscriberIceClick,
                    onRestartPublisherIceClick,
                    onSwitchSfuClick,
                    onSfuRejoinClick,
                    onSfuFastReconnectClick,
                    onSimulateSfuFullClick,
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
                    selectedRecordingTypes,
                    onSelectRecordingType,
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
                icon = menuIcon(ComposeR.drawable.stream_design_ic_caption_fill),
                action = onToggleClosedCaptions,
            )
        }

        is ClosedCaptionUiState.Running -> {
            ActionMenuItem(
                title = "Stop Closed Caption",
                icon = menuIcon(ComposeR.drawable.stream_design_ic_caption_fill),
                highlight = true,
                action = onToggleClosedCaptions,
            )
        }

        is ClosedCaptionUiState.UnAvailable -> {
            ActionMenuItem(
                title = "Closed Caption are unavailable",
                icon = menuIcon(ComposeR.drawable.stream_design_ic_no_sign),
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
            icon = menuIcon(ComposeR.drawable.stream_design_ic_file),
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
    onSimulateSfuFullClick: () -> Unit,
) = listOf(
    ActionMenuItem(
        title = "Publisher - ICE restart",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_refresh),
        action = onRestartPublisherIceClick,
    ),
    ActionMenuItem(
        title = "Subscriber - ICE restart",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_refresh),
        action = onRestartSubscriberIceClick,
    ),
    ActionMenuItem(
        title = "Reconnect SFU - migrate",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_left_to_right),
        action = onSwitchSfuClick,
    ),
    ActionMenuItem(
        title = "Reconnect SFU - rejoin",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_retry),
        action = onSfuRejoinClick,
    ),
    ActionMenuItem(
        title = "Reconnect SFU - fast",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_refresh),
        action = onSfuFastReconnectClick,
    ),
    ActionMenuItem(
        title = "Simulate SFU Full (700)",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_feedback),
        action = onSimulateSfuFullClick,
    ),
)

fun scaleTypeMenu(onSelectScaleType: (VideoScalingType) -> Unit): List<MenuItem> = listOf(
    ActionMenuItem(
        title = "Scale FIT",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_fullscreen_fill),
        action = { onSelectScaleType(VideoScalingType.SCALE_ASPECT_FIT) },
    ),
    ActionMenuItem(
        title = "Scale FILL",
        icon = menuIcon(Icons.Default.Crop),
        action = { onSelectScaleType(VideoScalingType.SCALE_ASPECT_FILL) },
    ),
    ActionMenuItem(
        title = "Scale BALANCED",
        icon = menuIcon(Icons.Default.Balance),
        action = { onSelectScaleType(VideoScalingType.SCALE_ASPECT_BALANCED) },
    ),
)

fun recordingTypeMenu(onSelectRecording: (RecordingType) -> Unit, selectedRecordingTypes: Set<RecordingType>): List<MenuItem> {
    return arrayListOf(
        ActionMenuItem(
            title = if (selectedRecordingTypes.contains(RecordingType.Raw)) {
                "Stop raw recording"
            } else {
                "Start raw recording"
            },
            icon = if (selectedRecordingTypes.contains(RecordingType.Raw)) {
                menuIcon(ComposeR.drawable.stream_design_ic_recording_fill)
            } else {
                menuIcon(ComposeR.drawable.stream_design_ic_recording_stop_fill)
            },
            highlight = selectedRecordingTypes.contains(RecordingType.Raw),
            action = { onSelectRecording(RecordingType.Raw) },
        ),
        ActionMenuItem(
            title = if (selectedRecordingTypes.contains(
                    RecordingType.Individual,
                )
            ) {
                "Stop individual recording"
            } else {
                "Start individual recording"
            },
            icon = menuIcon(ComposeR.drawable.stream_design_ic_user),
            highlight = selectedRecordingTypes.contains(RecordingType.Individual),
            action = { onSelectRecording(RecordingType.Individual) },
        ),
        ActionMenuItem(
            title = if (selectedRecordingTypes.contains(
                    RecordingType.Composite,
                )
            ) {
                "Stop composite recording"
            } else {
                "Start composite recording"
            },
            icon = menuIcon(ComposeR.drawable.stream_design_ic_fullscreen_fill),
            highlight = selectedRecordingTypes.contains(RecordingType.Composite),
            action = { onSelectRecording(RecordingType.Composite) },
        ),
    )
}

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
    onSimulateSfuFullClick: () -> Unit,
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
    selectedRecordingTypes: Set<RecordingType>,
    onSelectRecordingType: (RecordingType) -> Unit,
) = listOf(
    DynamicSubMenuItem(
        title = "List Transcriptions",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_spreadsheet),
        itemsLoader = loadTranscriptions,
    ),
    SubMenuItem(
        title = "Incoming video settings",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_settings_fill),
        items = listOf(
            ActionMenuItem(
                title = "Auto Quality",
                icon = menuIcon(Icons.Default.AspectRatio),
                highlight = selectedIncomingVideoResolution == null,
                action = { onSelectIncomingVideoResolution(null) },
            ),
            ActionMenuItem(
                title = "4K 2160p",
                icon = menuIcon(Icons.Default.AspectRatio),
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(3840, 2160),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(3840, 2160))
                },
            ),
            ActionMenuItem(
                title = "Full HD 1080p",
                icon = menuIcon(Icons.Default.AspectRatio),
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(1920, 1080),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(1920, 1080))
                },
            ),
            ActionMenuItem(
                title = "HD 720p",
                icon = menuIcon(Icons.Default.AspectRatio),
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(1280, 720),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(1280, 720))
                },
            ),
            ActionMenuItem(
                title = "SD 480p",
                icon = menuIcon(Icons.Default.AspectRatio),
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(640, 480),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(640, 480))
                },
            ),
            ActionMenuItem(
                title = "Data Saver 144p",
                icon = menuIcon(Icons.Default.AspectRatio),
                highlight = selectedIncomingVideoResolution == PreferredVideoResolution(256, 144),
                action = {
                    onSelectIncomingVideoResolution(PreferredVideoResolution(256, 144))
                },
            ),
            ActionMenuItem(
                title = if (isIncomingVideoEnabled) "Disable incoming video" else "Enable incoming video",
                icon = if (isIncomingVideoEnabled) {
                    menuIcon(
                        ComposeR.drawable.stream_design_ic_video_off_fill,
                    )
                } else {
                    menuIcon(ComposeR.drawable.stream_design_ic_video_fill)
                },
                action = { onToggleIncomingVideoEnabled(!isIncomingVideoEnabled) },
            ),
        ),
    ),
    SubMenuItem(
        title = "Scale type",
        icon = menuIcon(Icons.Default.AspectRatio),
        items = scaleTypeMenu(
            onSelectScaleType,
        ),
    ),
    SubMenuItem(
        title = "Available video codecs",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_code),
        items = codecMenu(codecList, onCodecSelected),
    ),
    ActionMenuItem(
        title = "Toggle audio filter",
        icon = menuIcon(CommonR.drawable.stream_video_ic_music_note),
        action = onToggleAudioFilterClick,
    ),
    ActionMenuItem(
        title = audioUsageUiState.text,
        icon = audioUsageUiState.icon,
        highlight = audioUsageUiState.highlight,
        action = onToggleAudioUsage,
    ),
    SubMenuItem(
        title = "Start/stop recording",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_recording_fill),
        items = recordingTypeMenu(onSelectRecordingType, selectedRecordingTypes),
    ),
    DynamicSubMenuItem(
        title = "Recordings",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_record_library_fill),
        itemsLoader = loadRecordings,
    ),
    ActionMenuItem(
        title = "Feedback",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_feedback),
        action = onShowFeedback,
    ),
    SubMenuItem(
        title = "Reconnect V2",
        icon = menuIcon(ComposeR.drawable.stream_design_ic_retry),
        items = reconnectMenu(
            onRestartPublisherIceClick,
            onRestartSubscriberIceClick,
            onSwitchSfuClick,
            onSfuRejoinClick,
            onSfuFastReconnectClick,
            onSimulateSfuFullClick,
        ),
    ),
)
