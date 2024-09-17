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
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.media.MediaCodecList
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.audio.AudioFilter
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.ui.call.ReactionsMenu
import io.getstream.video.android.ui.menu.base.ActionMenuItem
import io.getstream.video.android.ui.menu.base.DynamicMenu
import io.getstream.video.android.ui.menu.base.MenuItem
import io.getstream.video.android.util.filters.SampleAudioFilter
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun SettingsMenu(
    call: Call,
    selectedVideoFilter: Int,
    showDebugOptions: Boolean,
    onDismissed: () -> Unit,
    onSelectVideoFilter: (Int) -> Unit,
    onShowFeedback: () -> Unit,
    onShowCallStats: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()

    val screenSharePermissionResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                call.startScreenSharing(it.data!!)
            }
            onDismissed.invoke()
        },
    )

    val isScreenSharing by call.screenShare.isEnabled.collectAsStateWithLifecycle()
    val onScreenShareClick: () -> Unit = {
        if (!isScreenSharing) {
            scope.launch {
                val mediaProjectionManager =
                    context.getSystemService(MediaProjectionManager::class.java)
                screenSharePermissionResult.launch(
                    mediaProjectionManager.createScreenCaptureIntent(),
                )
            }
        } else {
            call.stopScreenSharing()
        }
    }

    val onToggleAudioFilterClick: () -> Unit = {
        if (call.audioFilter == null) {
            call.audioFilter = object : AudioFilter {
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

    val onKillSfuWsClick: () -> Unit = {
        call.debug.doFullReconnection()
        onDismissed.invoke()
        Toast.makeText(context, "Killing SFU WS. Should trigger reconnect...", Toast.LENGTH_SHORT)
            .show()
    }

    val onSwitchSfuClick: () -> Unit = {
        call.debug.switchSfu()
        onDismissed.invoke()
        Toast.makeText(context, "Switch sfu", Toast.LENGTH_SHORT).show()
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
                onToggleScreenShare = onScreenShareClick,
                onKillSfuWsClick = onKillSfuWsClick,
                onRestartPublisherIceClick = onRestartPublisherIceClick,
                onRestartSubscriberIceClick = onRestartSubscriberIceClick,
                onToggleAudioFilterClick = onToggleAudioFilterClick,
                onSwitchSfuClick = onSwitchSfuClick,
                onShowCallStats = onShowCallStats,
                isScreenShareEnabled = isScreenSharing,
                loadRecordings = onLoadRecordings,
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
                isScreenShareEnabled = false,
                onToggleScreenShare = { },
                onShowCallStats = { },
                onToggleAudioFilterClick = { },
                onRestartSubscriberIceClick = { },
                onRestartPublisherIceClick = { },
                onKillSfuWsClick = { },
                onSwitchSfuClick = { },
                availableDevices = emptyList(),
                onDeviceSelected = {},
                onShowFeedback = {},
                loadRecordings = { emptyList() },
            ),
        )
    }
}
