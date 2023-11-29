/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.audio.AudioFilter
import io.getstream.video.android.core.call.video.BitmapVideoFilter
import io.getstream.video.android.ui.common.R
import io.getstream.video.android.util.BlurredBackgroundVideoFilter
import io.getstream.video.android.util.SampleAudioFilter
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@Composable
internal fun SettingsMenu(
    call: Call,
    showDebugOptions: Boolean,
    onDisplayAvailableDevice: () -> Unit,
    onDismissed: () -> Unit,
    onShowReactionsMenu: () -> Unit,
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

    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(30, -200),
        onDismissRequest = { onDismissed.invoke() },
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .background(VideoTheme.colors.appBackground)
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.clickable {
                        if (call.videoFilter == null) {
                            call.videoFilter = object : BitmapVideoFilter() {
                                override fun filter(bitmap: Bitmap) {
                                    val filter = BlurredBackgroundVideoFilter()
                                    filter.applyFilter(bitmap)
                                }
                            }
                        } else {
                            call.videoFilter = null
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.stream_video_ic_fullscreen_exit,
                        ),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null,
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Toggle background blur (beta)",
                        color = VideoTheme.colors.textHighEmphasis,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        onDismissed()
                        onShowReactionsMenu()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_reaction),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null,
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Reactions",
                        color = VideoTheme.colors.textHighEmphasis,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        if (!isScreenSharing) {
                            scope.launch {
                                val mediaProjectionManager = context.getSystemService(
                                    MediaProjectionManager::class.java,
                                )
                                screenSharePermissionResult.launch(
                                    mediaProjectionManager.createScreenCaptureIntent(),
                                )
                            }
                        } else {
                            call.stopScreenSharing()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_screensharing),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null,
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = screenShareButtonText,
                        color = VideoTheme.colors.textHighEmphasis,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (showDebugOptions) {
                    Row(
                        modifier = Modifier.clickable {
                            if (call.audioFilter == null) {
                                call.audioFilter = object : AudioFilter {
                                    override fun filter(
                                        audioFormat: Int,
                                        channelCount: Int,
                                        sampleRate: Int,
                                        sampleData: ByteBuffer,
                                    ) {
                                        SampleAudioFilter.toRoboticVoice(
                                            sampleData,
                                            channelCount,
                                            0.8f,
                                        )
                                    }
                                }
                            } else {
                                call.audioFilter = null
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.stream_video_ic_fullscreen_exit,
                            ),
                            tint = VideoTheme.colors.textHighEmphasis,
                            contentDescription = null,
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = "Toggle audio filter",
                            color = VideoTheme.colors.textHighEmphasis,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.clickable {
                            call.debug.restartSubscriberIce()
                            onDismissed.invoke()
                            Toast.makeText(
                                context,
                                "Restart Subscriber Ice",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.stream_video_ic_fullscreen_exit,
                            ),
                            tint = VideoTheme.colors.textHighEmphasis,
                            contentDescription = null,
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = "Restart Subscriber Ice",
                            color = VideoTheme.colors.textHighEmphasis,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.clickable {
                            call.debug.restartPublisherIce()
                            onDismissed.invoke()
                            Toast.makeText(
                                context,
                                "Restart Publisher Ice",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.stream_video_ic_fullscreen_exit,
                            ),
                            tint = VideoTheme.colors.textHighEmphasis,
                            contentDescription = null,
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = "Restart Publisher Ice",
                            color = VideoTheme.colors.textHighEmphasis,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.clickable {
                            call.debug.doFullReconnection()
                            onDismissed.invoke()
                            Toast.makeText(
                                context,
                                "Killing SFU WS. Should trigger reconnect...",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.stream_video_ic_fullscreen_exit,
                            ),
                            tint = VideoTheme.colors.textHighEmphasis,
                            contentDescription = null,
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = "Kill SFU WS",
                            color = VideoTheme.colors.textHighEmphasis,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.clickable {
                            call.debug.switchSfu()
                            onDismissed.invoke()
                            Toast.makeText(context, "Switch sfu", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stream_video_ic_fullscreen),
                            tint = VideoTheme.colors.textHighEmphasis,
                            contentDescription = null,
                        )

                        Text(
                            modifier = Modifier.padding(start = 20.dp),
                            text = "Switch sfu",
                            color = VideoTheme.colors.textHighEmphasis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        onDismissed.invoke()
                        onDisplayAvailableDevice.invoke()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_mic_on),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null,
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Switch Microphone",
                        color = VideoTheme.colors.textHighEmphasis,
                    )
                }
            }
        }
    }
}
