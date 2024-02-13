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
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.PortableWifiOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoSettings
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.ui.menu.base.ActionMenuItem
import io.getstream.video.android.ui.menu.base.MenuItem
import io.getstream.video.android.ui.menu.base.SubMenuItem

fun defaultStreamMenu(
    showDebugOptions: Boolean = false,
    codecList: List<MediaCodecInfo>,
    onCodecSelected: (MediaCodecInfo) -> Unit,
    isScreenShareEnabled: Boolean,
    isBackgroundBlurEnabled: Boolean,
    onToggleScreenShare: () -> Unit = {},
    onShowCallStats: () -> Unit,
    onToggleBackgroundBlurClick: () -> Unit,
    onToggleAudioFilterClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onRestartPublisherIceClick: () -> Unit,
    onKillSfuWsClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
    onDeviceSelected: (StreamAudioDevice) -> Unit,
    availableDevices: List<StreamAudioDevice>,
) = buildList<MenuItem> {
    add(
        ActionMenuItem(
            title = if (isScreenShareEnabled) "Stop screen-share" else "Start screen-share",
            icon = if (isScreenShareEnabled) Icons.AutoMirrored.Default.StopScreenShare else Icons.AutoMirrored.Default.ScreenShare,
            action = onToggleScreenShare,
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
        ActionMenuItem(
            title = if (isBackgroundBlurEnabled) "Disable background blur" else "Enable background blur",
            icon = if (isBackgroundBlurEnabled) Icons.Default.BlurOff else Icons.Default.BlurOn,
            action = onToggleBackgroundBlurClick,
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
}

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
