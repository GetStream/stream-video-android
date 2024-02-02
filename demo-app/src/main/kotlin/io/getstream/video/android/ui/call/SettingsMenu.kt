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

package io.getstream.video.android.ui.call

import android.app.Activity
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.audio.AudioFilter
import io.getstream.video.android.core.call.video.BitmapVideoFilter
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.tooling.extensions.toPx
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.util.BlurredBackgroundVideoFilter
import io.getstream.video.android.util.SampleAudioFilter
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

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
    val screenShareButtonText = if (isScreenSharing) {
        "Stop screen-sharing"
    } else {
        "Start screen-sharing"
    }

    // Define the actions as lambda variables
    val onReactionsClick: () -> Unit = {
        onDismissed()
        onShowReactionsMenu()
    }

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

    val onSwitchMicrophoneClick: () -> Unit = {
        onDismissed.invoke()
        onDisplayAvailableDevice.invoke()
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

    SettingsMenuItems(
        onScreenShareClick = onScreenShareClick,
        onSwitchMicrophoneClick = onSwitchMicrophoneClick,
        onToggleBackgroundBlurClick = onToggleBackgroundBlurClick,
        onToggleAudioFilterClick = onToggleAudioFilterClick,
        onRestartSubscriberIceClick = onRestartSubscriberIceClick,
        onRestartPublisherIceClick = onRestartPublisherIceClick,
        onKillSfuWsClick = onKillSfuWsClick,
        onSwitchSfuClick = onSwitchSfuClick,
        showDebugOptions = showDebugOptions,
        isBackgroundBlurEnabled = isBackgroundBlurEnabled,
        onShowStates = onShowCallStats,
        onDismissed = onDismissed,
        reactionsMenu = {
            ReactionsMenu(call = call, reactionMapper = ReactionMapper.defaultReactionMapper()) {
                onDismissed()
            }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsMenuItems(
    isBackgroundBlurEnabled: Boolean,
    onScreenShareClick: () -> Unit,
    onSwitchMicrophoneClick: () -> Unit,
    onToggleBackgroundBlurClick: () -> Unit,
    onToggleAudioFilterClick: () -> Unit,
    onRestartSubscriberIceClick: () -> Unit,
    onRestartPublisherIceClick: () -> Unit,
    onKillSfuWsClick: () -> Unit,
    onSwitchSfuClick: () -> Unit,
    showDebugOptions: Boolean,
    onDismissed: () -> Unit,
    reactionsMenu: @Composable () -> Unit,
    onShowStates: () -> Unit,
) {
    Popup(
        offset = IntOffset(0, -VideoTheme.dimens.generic3xl.toPx().toInt()),
        alignment = Alignment.BottomStart,
        onDismissRequest = { onDismissed.invoke() },
        properties = PopupProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = VideoTheme.shapes.sheet,
                    color = VideoTheme.colors.baseSheetPrimary,
                )
                .padding(12.dp),
        ) {
            reactionsMenu()
            Spacer(modifier = Modifier.size(VideoTheme.dimens.spacingM))

            MenuEntry(
                icon = R.drawable.stream_video_ic_screensharing,
                label = "Toggle screen-sharing",
                onClick = onScreenShareClick,
            )
            MenuEntry(
                vector = Icons.Default.ShowChart,
                icon = io.getstream.video.android.R.drawable.ic_layout_grid,
                label = "Call stats",
                onClick = onShowStates,
            )
            MenuEntry(
                icon = io.getstream.video.android.R.drawable.ic_mic,
                label = "Switch Microphone",
                onClick = onSwitchMicrophoneClick,
            )
            MenuEntry(
                icon = if (isBackgroundBlurEnabled) io.getstream.video.android.R.drawable.ic_blur_off else io.getstream.video.android.R.drawable.ic_blur_on,
                label = if (isBackgroundBlurEnabled) "Disable background blur" else "Enable background blur (beta)",
                onClick = onToggleBackgroundBlurClick,
            )
            if (showDebugOptions) {
                MenuEntry(
                    icon = R.drawable.stream_video_ic_fullscreen_exit,
                    label = "Toggle audio filter",
                    onClick = onToggleAudioFilterClick,
                )
                MenuEntry(
                    icon = R.drawable.stream_video_ic_fullscreen_exit,
                    label = "Restart Subscriber Ice",
                    onClick = onRestartSubscriberIceClick,
                )
                MenuEntry(
                    icon = R.drawable.stream_video_ic_fullscreen_exit,
                    label = "Restart Publisher Ice",
                    onClick = onRestartPublisherIceClick,
                )
                MenuEntry(
                    icon = R.drawable.stream_video_ic_fullscreen_exit,
                    label = "Kill SFU WS",
                    onClick = onKillSfuWsClick,
                )
                MenuEntry(
                    icon = R.drawable.stream_video_ic_fullscreen,
                    label = "Switch SFU",
                    onClick = onSwitchSfuClick,
                )
            }
        }
    }
}

@Composable
private fun MenuEntry(
    vector: ImageVector? = null,
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) = StreamToggleButton(
    onText = label,
    offText = label,
    onIcon = vector ?: ImageVector.vectorResource(icon),
    onStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOn(StyleSize.XS).copy(
        iconStyle = VideoTheme.styles.iconStyles.customColorIconStyle(
            color = VideoTheme.colors.basePrimary,
        ),
    ),
) { onClick() }

@Preview
@Composable
private fun SettingsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SettingsMenuItems(
            isBackgroundBlurEnabled = true,
            onScreenShareClick = { },
            onSwitchMicrophoneClick = { },
            onToggleBackgroundBlurClick = { },
            onToggleAudioFilterClick = { },
            onRestartSubscriberIceClick = { },
            onRestartPublisherIceClick = { },
            onKillSfuWsClick = { },
            onSwitchSfuClick = { },
            showDebugOptions = true,
            onDismissed = { },
            onShowStates = { },
            reactionsMenu = {
                ReactionsMenu(
                    call = previewCall,
                    reactionMapper = ReactionMapper.defaultReactionMapper(),
                ) {
                }
            },
        )
    }
}
