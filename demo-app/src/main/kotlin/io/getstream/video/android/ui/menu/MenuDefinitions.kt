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
import androidx.compose.material.icons.automirrored.filled.MobileScreenShare
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.PortableWifiOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.SpatialAudioOff
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VideoSettings
import io.getstream.video.android.core.TranscriptionState
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.ui.menu.base.ActionMenuItem
import io.getstream.video.android.ui.menu.base.DynamicSubMenuItem
import io.getstream.video.android.ui.menu.base.MenuItem
import io.getstream.video.android.ui.menu.base.SubMenuItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Defines the default Stream menu for the demo app.
 */
fun defaultStreamMenu(
    showDebugOptions: Boolean = false,
    noiseCancellationFeatureEnabled: Boolean = false,
    noiseCancellationEnabled: Boolean = false,
    codecList: List<MediaCodecInfo>,
    onCodecSelected: (MediaCodecInfo) -> Unit,
    isScreenShareEnabled: Boolean,
    onToggleScreenShare: () -> Unit = {},
    onShowCallStats: () -> Unit,
    onToggleAudioFilterClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onRestartPublisherIceClick: () -> Unit,
    onKillSfuWsClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
    onShowFeedback: () -> Unit,
    onNoiseCancellation: () -> Unit,
    onDeviceSelected: (StreamAudioDevice) -> Unit,
    availableDevices: List<StreamAudioDevice>,
    loadRecordings: suspend () -> List<MenuItem>,
    transcriptionState: TranscriptionState,
    onToggleTranscription: suspend () -> Unit,
    transcriptionList: suspend () -> List<MenuItem>,
) = buildList<MenuItem> {
    add(
        DynamicSubMenuItem(
            title = "Recordings",
            icon = Icons.Default.VideoLibrary,
            itemsLoader = loadRecordings,
        ),
    )
    add(
        ActionMenuItem(
            title = "Call stats",
            icon = Icons.Default.AutoGraph,
            action = onShowCallStats,
        ),
    )
    add(
        SubMenuItem(
            title = "Choose audio device",
            icon = Icons.Default.SettingsVoice,
            items = availableDevices.map {
                val icon = when (it) {
                    is StreamAudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
                    is StreamAudioDevice.Earpiece -> Icons.Default.Headphones
                    is StreamAudioDevice.Speakerphone -> Icons.Default.SpeakerPhone
                    is StreamAudioDevice.WiredHeadset -> Icons.Default.HeadsetMic
                }
                ActionMenuItem(
                    title = it.name,
                    icon = icon,
                    action = { onDeviceSelected(it) },
                )
            },
        ),
    )
    add(
        ActionMenuItem(
            title = "Feedback",
            icon = Icons.Default.Feedback,
            action = onShowFeedback,
        ),
    )
    add(
        ActionMenuItem(
            title = if (isScreenShareEnabled) "Stop screen-share" else "Start screen-share",
            icon = Icons.AutoMirrored.Default.MobileScreenShare,
            action = onToggleScreenShare,
        ),
    )
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
                    onKillSfuWsClick,
                    onSwitchSfuClick,
                ),
            ),
        )
    }
    val transcriptionUiState = transcriptionState.mapTouUiState()
    add(ActionMenuItem(
        title = transcriptionUiState.text,
        icon = transcriptionUiState.icon,
        action = {
            GlobalScope.launch {
                onToggleTranscription.invoke()
            }
        },
    ))
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

/**
 * Optionally defines the debug sub-menu of the demo app.
 */
fun debugSubmenu(
    codecList: List<MediaCodecInfo>,
    onCodecSelected: (MediaCodecInfo) -> Unit,
    onToggleAudioFilterClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onRestartPublisherIceClick: () -> Unit,
    onKillSfuWsClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
) = listOf(
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
        title = "Restart subscriber Ice",
        icon = Icons.Default.RestartAlt,
        action = onRestartSubscriberIceClick,
    ),
    ActionMenuItem(
        title = "Restart publisher Ice",
        icon = Icons.Default.RestartAlt,
        action = onRestartPublisherIceClick,
    ),
    ActionMenuItem(
        title = "Shut down SFU web-socket",
        icon = Icons.Default.PortableWifiOff,
        action = onKillSfuWsClick,
    ),
    ActionMenuItem(
        title = "Switch SFU",
        icon = Icons.Default.SwitchLeft,
        action = onSwitchSfuClick,
    ),
)