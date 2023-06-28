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

package io.getstream.video.android.dogfooding.ui.call

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

@Composable
fun CallScreen(
    call: Call,
    onBackPressed: () -> Unit = {},
    onLeaveCall: () -> Unit = {}
) {
    VideoTheme {
        CallContent(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            call = call,
            enableInPictureInPicture = true,
            onBackPressed = { onBackPressed.invoke() },
            onCallAction = { callAction ->
                when (callAction) {
                    is FlipCamera -> call.camera.flip()
                    is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
                    is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
                    is LeaveCall -> onLeaveCall.invoke()
                    else -> Unit
                }
            }
        )
    }
}

@Preview
@Composable
private fun CallScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallScreen(call = mockCall)
    }
}
