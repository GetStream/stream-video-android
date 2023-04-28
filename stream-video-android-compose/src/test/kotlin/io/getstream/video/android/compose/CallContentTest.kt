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

package io.getstream.video.android.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.common.util.mockParticipants
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.call.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.incomingcall.internal.IncomingCallControls
import io.getstream.video.android.compose.ui.components.call.incomingcall.internal.IncomingCallDetails
import io.getstream.video.android.compose.ui.components.call.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.call.outgoingcall.internal.OutgoingCallControls
import io.getstream.video.android.compose.ui.components.call.outgoingcall.internal.OutgoingCallDetails
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.CallType
import org.junit.Rule
import org.junit.Test

internal class CallContentTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot IncomingCallContentDetails Video composable`() {
        snapshot {
            IncomingCallDetails(
                callType = CallType.VIDEO,
                participants = mockParticipantList
            )
        }
    }

    @Test
    fun `snapshot IncomingCallContentDetails Audio composable`() {
        snapshot {
            IncomingCallDetails(
                callType = CallType.AUDIO,
                participants = mockParticipantList
            )
        }
    }

    @Test
    fun `snapshot IncomingCallOptions composable`() {
        snapshotWithDarkMode {
            IncomingCallControls(
                isVideoCall = true,
                isCameraEnabled = true,
                onCallAction = { }
            )
        }
    }

    @Test
    fun `snapshot IncomingCallContent Video type with one participant composable`() {
        snapshot {
            IncomingCallContent(
                participants = mockParticipants.takeLast(1),
                callType = CallType.VIDEO,
                isCameraEnabled = false,
                onBackPressed = {}
            ) {}
        }
    }

    @Test
    fun `snapshot IncomingCallContent Video type with multiple participants composable`() {
        snapshot {
            IncomingCallContent(
                participants = mockParticipants,
                callType = CallType.VIDEO,
                isCameraEnabled = false,
                onBackPressed = {}
            ) {}
        }
    }

    @Test
    fun `snapshot OutgoingCallDetails Video composable`() {
        snapshot {
            OutgoingCallDetails(
                callType = CallType.VIDEO,
                participants = mockParticipants
            )
        }
    }

    @Test
    fun `snapshot OutgoingCallDetails Audio composable`() {
        snapshot {
            OutgoingCallDetails(
                callType = CallType.VIDEO,
                participants = mockParticipants
            )
        }
    }

    @Test
    fun `snapshot OutgoingCallOptions composable`() {
        snapshotWithDarkMode {
            Column {
                OutgoingCallControls(
                    callDeviceState = CallDeviceState(
                        isMicrophoneEnabled = true,
                        isSpeakerphoneEnabled = true,
                        isCameraEnabled = true
                    ),
                    onCallAction = { }
                )
                OutgoingCallControls(
                    callDeviceState = CallDeviceState(),
                    onCallAction = { }
                )
            }
        }
    }

    @Test
    fun `snapshot OutgoingCallContent Video type with one participant composable`() {
        snapshot {
            OutgoingCallContent(
                callType = CallType.VIDEO,
                participants = mockParticipants.take(1),
                callDeviceState = CallDeviceState(),
                modifier = Modifier.fillMaxSize(),
                onBackPressed = {},
                onCallAction = {}
            )
        }
    }

    @Test
    fun `snapshot OutgoingCallContent Video type with multiple participants composable`() {
        snapshot {
            OutgoingCallContent(
                callType = CallType.VIDEO,
                participants = mockParticipants,
                callDeviceState = CallDeviceState(),
                onBackPressed = {}
            ) {}
        }
    }
}
