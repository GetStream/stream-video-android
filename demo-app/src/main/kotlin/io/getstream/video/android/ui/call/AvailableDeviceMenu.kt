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

package io.getstream.video.android.ui.call

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import kotlinx.coroutines.delay

@Composable
fun AvailableDeviceMenu(
    call: Call,
    onDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = availableDevices) {
        delay(3000)
        if (availableDevices.isEmpty()) {
            onDismissed.invoke()
        } else {
            Toast.makeText(context, "There's no available devices", Toast.LENGTH_SHORT).show()
        }
    }

    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(30, -200),
        onDismissRequest = { onDismissed.invoke() },
    ) {
        Card(
            modifier = Modifier.width(140.dp),
            shape = RoundedCornerShape(12.dp),
            contentColor = VideoTheme.colors.basePrimary,
            backgroundColor = VideoTheme.colors.baseSheetPrimary,
            elevation = 6.dp,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
            ) {
                items(items = availableDevices, key = { it.name }) { audioDevice ->
                    Text(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clickable {
                                call.microphone.select(audioDevice)
                                onDismissed.invoke()
                                Toast
                                    .makeText(
                                        context,
                                        "Switched to ${audioDevice.name}",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            },
                        text = audioDevice.name,
                        color = VideoTheme.colors.basePrimary,
                    )
                }
            }
        }
    }
}
