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

import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.R
import kotlinx.coroutines.launch

@Composable
internal fun SettingsMenu(
    call: Call,
    onDisplayAvailableDevice: () -> Unit,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reactions =
        listOf(":fireworks:", ":hello:", ":raise-hand:", ":like:", ":hate:", ":smile:", ":heart:")

    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(30, -200),
        onDismissRequest = { onDismissed.invoke() }
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .background(VideoTheme.colors.appBackground)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.clickable {
                        scope.launch {
                            val shuffled = reactions.shuffled()
                            call.sendReaction(type = "default", emoji = shuffled.first())
                            onDismissed.invoke()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_reaction),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Reactions",
                        color = VideoTheme.colors.textHighEmphasis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        call.debug.restartSubscriberIce()
                        onDismissed.invoke()
                        Toast.makeText(context, "Restart Subscriber Ice", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_fullscreen_exit),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Restart Subscriber Ice",
                        color = VideoTheme.colors.textHighEmphasis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        call.debug.restartPublisherIce()
                        onDismissed.invoke()
                        Toast.makeText(context, "Restart Publisher Ice", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_fullscreen_exit),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Restart Publisher Ice",
                        color = VideoTheme.colors.textHighEmphasis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        call.debug.switchSfu()
                        onDismissed.invoke()
                        Toast.makeText(context, "Switch sfu", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_fullscreen),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Switch sfu",
                        color = VideoTheme.colors.textHighEmphasis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        onDismissed.invoke()
                        onDisplayAvailableDevice.invoke()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.stream_video_ic_mic_on),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Switch Microphone",
                        color = VideoTheme.colors.textHighEmphasis
                    )
                }
            }
        }
    }
}