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

package io.getstream.video.android.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallControls
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallDetails
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallControls
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallDetails
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewMemberListState
import org.junit.Rule
import org.junit.Test

internal class CallContentTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot IncomingCallContentDetails Video composable`() {
        snapshot {
            IncomingCallDetails(participants = previewMemberListState)
        }
    }

    @Test
    fun `snapshot IncomingCallContentDetails Audio composable`() {
        snapshot {
            IncomingCallDetails(participants = previewMemberListState)
        }
    }

    @Test
    fun `snapshot IncomingCallOptions composable`() {
        snapshotWithDarkMode {
            IncomingCallControls(
                isVideoCall = true,
                isCameraEnabled = true,
                onCallAction = { },
            )
        }
    }

    @Test
    fun `snapshot IncomingCallContent with one participant composable`() {
        snapshot {
            IncomingCallContent(
                call = previewCall,
                participants = previewMemberListState.takeLast(1),
                isCameraEnabled = false,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `snapshot IncomingCallContent Video type with multiple participants composable`() {
        snapshot {
            IncomingCallContent(
                call = previewCall,
                participants = previewMemberListState,
                isCameraEnabled = false,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `snapshot IncomingCallContent with minimum parameters`() {
        snapshot {
            IncomingCallContent(
                call = previewCall,
            )
        }
    }

    @Test
    fun `snapshot OutgoingCallDetails Video composable`() {
        snapshot {
            OutgoingCallDetails(participants = previewMemberListState)
        }
    }

    @Test
    fun `snapshot OutgoingCallDetails Audio composable`() {
        snapshot {
            OutgoingCallDetails(participants = previewMemberListState)
        }
    }

    @Test
    fun `snapshot OutgoingCallOptions composable`() {
        snapshotWithDarkMode {
            Column {
                OutgoingCallControls(
                    isMicrophoneEnabled = true,
                    isCameraEnabled = true,
                    onCallAction = { },
                )
                OutgoingCallControls(
                    isMicrophoneEnabled = false,
                    isCameraEnabled = false,
                    onCallAction = { },
                )
            }
        }
    }

    @Test
    fun `snapshot OutgoingCallContent with one participant composable`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                participants = previewMemberListState.take(1),
                modifier = Modifier.fillMaxSize(),
                onBackPressed = {},
                onCallAction = {},
            )
        }
    }

    @Test
    fun `snapshot OutgoingCallContent with multiple participants composable`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                participants = previewMemberListState,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `snapshot CallContent with multiple participants composable`() {
        snapshot {
            CallContent(call = previewCall)
        }
    }

    @Test
    fun `snapshot OutgoingCallContent with minimum parameters and video type`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                isVideoType = true,
            )
        }
    }

    @Test
    fun `snapshot OutgoingCallContent with minimum parameters and audio type`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                isVideoType = false,
            )
        }
    }
}
