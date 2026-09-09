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

package io.getstream.video.android.ui

import android.media.AudioAttributes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.util.StreamVideoInitHelper

/**
 * A row in the call settings screen: one call type and its current [CallServiceConfig].
 */
private data class CallTypeConfigItem(
    val callType: CallType,
    val label: String,
    val config: CallServiceConfig,
)

/** Selectable audio usage values exposed in the UI, mapped to [AudioAttributes] constants. */
private data class AudioUsageOption(val label: String, val value: Int)

private val audioUsageOptions = listOf(
    AudioUsageOption("Voice comm", AudioAttributes.USAGE_VOICE_COMMUNICATION),
    AudioUsageOption("Media", AudioAttributes.USAGE_MEDIA),
)

/** The call types the demo app exposes for editing. */
private val editableCallTypes = listOf(
    CallType.Default to "Default",
    CallType.AnyMarker to "All call types",
    CallType.Livestream to "Livestream",
    CallType.AudioCall to "Audio call",
    CallType.AudioRoom to "Audio room",
)

/**
 * A debug screen that lets the user edit the live [CallServiceConfigRegistry] used by the active
 * [io.getstream.video.android.core.StreamVideo] client.
 *
 * Each call type's configuration is read from the registry and any change is written straight
 * back to it via [CallServiceConfigRegistry.register], so it takes effect for subsequent calls.
 */
@Composable
fun CallSettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val registry = remember { StreamVideoInitHelper.callServiceConfigRegistry }

    // Intercept the system back press so it dismisses this overlay instead of finishing the activity.
    BackHandler(onBack = onClose)

    var items by remember {
        mutableStateOf(
            registry?.let { reg ->
                editableCallTypes.map { (callType, label) ->
                    CallTypeConfigItem(callType, label, reg.get(callType.name))
                }
            }.orEmpty(),
        )
    }

    fun updateConfig(callType: CallType, transform: (CallServiceConfig) -> CallServiceConfig) {
        val reg = registry ?: return
        items = items.map { item ->
            if (item.callType == callType) {
                val updated = transform(item.config)
                // Write the change back to the live registry. copy() preserves the
                // serviceClass and moderationConfig that the UI does not edit.
                reg.register(callType.name, updated)
                item.copy(config = updated)
            } else {
                item
            }
        }
    }

    CallSettingsContent(
        items = items,
        registryAvailable = registry != null,
        onClose = onClose,
        onForegroundChanged = { callType, enabled ->
            updateConfig(callType) { it.copy(runCallServiceInForeground = enabled) }
        },
        onTelecomChanged = { callType, enabled ->
            updateConfig(callType) { it.copy(enableTelecom = enabled) }
        },
        onAudioUsageChanged = { callType, usage ->
            updateConfig(callType) { it.copy(audioUsage = usage) }
        },
        modifier = modifier,
    )
}

/**
 * Stateless content for [CallSettingsScreen]. Kept separate from the stateful screen so it can be
 * rendered in a @Preview without a live [CallServiceConfigRegistry].
 */
@Composable
private fun CallSettingsContent(
    items: List<CallTypeConfigItem>,
    registryAvailable: Boolean,
    onClose: () -> Unit,
    onForegroundChanged: (CallType, Boolean) -> Unit,
    onTelecomChanged: (CallType, Boolean) -> Unit,
    onAudioUsageChanged: (CallType, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.backgroundCoreApp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VideoTheme.colors.backgroundCoreElevation1)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Call Settings",
                style = VideoTheme.typography.headingSmall,
                color = VideoTheme.colors.textPrimary,
            )

            Box(
                modifier = Modifier
                    .background(
                        color = VideoTheme.colors.backgroundCoreSurfaceDefault,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable(onClick = onClose)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = VideoTheme.colors.textPrimary,
                )
            }
        }

        if (!registryAvailable) {
            Text(
                text = "Call service config registry is not available yet. " +
                    "Sign in / start the SDK first.",
                modifier = Modifier.padding(16.dp),
                style = VideoTheme.typography.bodyDefault,
                color = VideoTheme.colors.textSecondary,
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.callType.name }) { item ->
                CallTypeConfigCard(
                    item = item,
                    onForegroundChanged = { enabled ->
                        onForegroundChanged(
                            item.callType,
                            enabled,
                        )
                    },
                    onTelecomChanged = { enabled -> onTelecomChanged(item.callType, enabled) },
                    onAudioUsageChanged = { usage -> onAudioUsageChanged(item.callType, usage) },
                )
            }
        }
    }
}

@Composable
private fun CallTypeConfigCard(
    item: CallTypeConfigItem,
    onForegroundChanged: (Boolean) -> Unit,
    onTelecomChanged: (Boolean) -> Unit,
    onAudioUsageChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = VideoTheme.colors.backgroundCoreElevation1,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Text(
            text = item.label,
            style = VideoTheme.typography.headingLarge,
            color = VideoTheme.colors.textPrimary,
        )
        Text(
            text = "type: ${item.callType.name}",
            modifier = Modifier.padding(top = 2.dp),
            style = VideoTheme.typography.bodyEmphasis,
            color = VideoTheme.colors.textSecondary,
        )
        Text(
            text = "service: ${item.config.serviceClass.simpleName}",
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            style = VideoTheme.typography.bodyEmphasis,
            color = VideoTheme.colors.textSecondary,
        )

        ToggleRow(
            label = "Run service in foreground",
            checked = item.config.runCallServiceInForeground,
            onCheckedChange = onForegroundChanged,
        )
        ToggleRow(
            label = "Enable Telecom",
            checked = item.config.enableTelecom,
            onCheckedChange = onTelecomChanged,
        )

        Text(
            text = "Audio usage",
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            style = VideoTheme.typography.captionDefault,
            color = VideoTheme.colors.textPrimary,
        )
        AudioUsageSelector(
            selected = item.config.audioUsage,
            onSelected = onAudioUsageChanged,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = VideoTheme.typography.captionDefault,
            color = VideoTheme.colors.textPrimary,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VideoTheme.colors.accentPrimary,
                checkedTrackColor = VideoTheme.colors.accentPrimary,
                uncheckedThumbColor = VideoTheme.colors.textSecondary,
                uncheckedTrackColor = VideoTheme.colors.backgroundCoreSurfaceDefault,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioUsageSelector(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        audioUsageOptions.forEach { option ->
            val isSelected = option.value == selected
            Text(
                text = option.label,
                modifier = Modifier
                    .background(
                        color = if (isSelected) {
                            VideoTheme.colors.accentPrimary
                        } else {
                            VideoTheme.colors.backgroundCoreSurfaceDefault
                        },
                        shape = RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelected(option.value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = VideoTheme.typography.captionDefault,
                color = if (isSelected) {
                    VideoTheme.colors.backgroundCoreApp
                } else {
                    VideoTheme.colors.textPrimary
                },
            )
        }
    }
}

private fun previewCallTypeConfigItems(): List<CallTypeConfigItem> = listOf(
    CallTypeConfigItem(
        callType = CallType.Default,
        label = "Default",
        config = CallServiceConfig(
            runCallServiceInForeground = true,
            audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
            enableTelecom = false,
        ),
    ),
    CallTypeConfigItem(
        callType = CallType.AudioCall,
        label = "Audio call",
        config = CallServiceConfig(
            runCallServiceInForeground = false,
            audioUsage = AudioAttributes.USAGE_MEDIA,
            enableTelecom = true,
        ),
    ),
    CallTypeConfigItem(
        callType = CallType.Livestream,
        label = "Livestream",
        config = CallServiceConfig(
            runCallServiceInForeground = true,
            audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
            enableTelecom = false,
        ),
    ),
)

@Preview(
    name = "Call Settings",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun CallSettingsContentPreview() {
    VideoTheme {
        CallSettingsContent(
            items = previewCallTypeConfigItems(),
            registryAvailable = true,
            onClose = {},
            onForegroundChanged = { _, _ -> },
            onTelecomChanged = { _, _ -> },
            onAudioUsageChanged = { _, _ -> },
        )
    }
}

@Preview(
    name = "Call Settings — registry unavailable",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun CallSettingsUnavailablePreview() {
    VideoTheme {
        CallSettingsContent(
            items = emptyList(),
            registryAvailable = false,
            onClose = {},
            onForegroundChanged = { _, _ -> },
            onTelecomChanged = { _, _ -> },
            onAudioUsageChanged = { _, _ -> },
        )
    }
}
