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

package io.getstream.video.android.compose.ui.components.call

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Preview
@Composable
@ExperimentalTime
private fun CallTopAppbarPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Column {
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = false,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = true,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = false,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = false,
                    isReconnecting = true,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
            CallAppBar(call = previewCall, centerContent = {
                CalLCenterContent(
                    text = 100000000L.milliseconds.toString(),
                    isRecording = true,
                    isReconnecting = false,
                )
            })
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
