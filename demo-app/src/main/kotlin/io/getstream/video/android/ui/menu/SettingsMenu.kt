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

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaCodecList
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.audio.AudioFilter
import io.getstream.video.android.core.call.video.BitmapVideoFilter
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.ui.call.ReactionsMenu
import io.getstream.video.android.ui.menu.base.DynamicMenu
import io.getstream.video.android.util.BlurredBackgroundVideoFilter
import io.getstream.video.android.util.SampleAudioFilter
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsMenu(
    call: Call,
    showDebugOptions: Boolean,
    isBackgroundBlurEnabled: Boolean,
    onDisplayAvailableDevice: () -> Unit,
    onDismissed: () -> Unit,
    onShowReactionsMenu: () -> Unit,
    onToggleBackgroundBlur: () -> Unit,
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

    val onToggleBackgroundBlurClick: () -> Unit = {
        onToggleBackgroundBlur()

        if (call.videoFilter == null) {
            call.videoFilter = object : BitmapVideoFilter() {
                val filter = BlurredBackgroundVideoFilter()

                override fun filter(bitmap: Bitmap) {
                    filter.applyFilter(bitmap)
                }
            }
        } else {
            call.videoFilter = null
        }
    }

    val onToggleAudioFilterClick: () -> Unit = {
        if (call.audioFilter == null) {
            call.audioFilter = object : AudioFilter {
                override fun filter(
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
                onToggleScreenShare = onScreenShareClick,
                onKillSfuWsClick = onKillSfuWsClick,
                onRestartPublisherIceClick = onRestartPublisherIceClick,
                onRestartSubscriberIceClick = onRestartSubscriberIceClick,
                onToggleAudioFilterClick = onToggleAudioFilterClick,
                onToggleBackgroundBlurClick = onToggleBackgroundBlurClick,
                onSwitchSfuClick = onSwitchSfuClick,
                onShowCallStats = onShowCallStats,
                isBackgroundBlurEnabled = isBackgroundBlurEnabled,
                isScreenShareEnabled = isScreenSharing,
            ),
        )
    }
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
                isBackgroundBlurEnabled = true,
                onToggleScreenShare = { },
                onShowCallStats = { },
                onToggleBackgroundBlurClick = { },
                onToggleAudioFilterClick = { },
                onRestartSubscriberIceClick = { },
                onRestartPublisherIceClick = { },
                onKillSfuWsClick = { },
                onSwitchSfuClick = { },
                availableDevices = emptyList(),
                onDeviceSelected = {
                },
            ),
        )
    }
}
