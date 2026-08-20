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
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import io.getstream.video.android.compose.ui.MAX_PERCENT_DIFFERENCE
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
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

internal class CallContentTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = MAX_PERCENT_DIFFERENCE,
    )

    @Test
    fun `incoming call details video`() {
        snapshotWithDarkMode {
            IncomingCallDetails(participants = previewMemberListState)
        }
    }

    @Test
    fun `incoming call details audio`() {
        snapshotWithDarkMode {
            IncomingCallDetails(
                isVideoType = false,
                participants = previewMemberListState,
            )
        }
    }

    @Test
    fun `incoming call options`() {
        snapshotWithDarkMode {
            IncomingCallControls(
                isVideoCall = true,
                isCameraEnabled = true,
                onCallAction = { },
            )
        }
    }

    @Test
    fun `incoming call content with one participant`() {
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
    fun `incoming call content with one participant in dark mode`() {
        snapshot(isInDarkMode = true) {
            IncomingCallContent(
                call = previewCall,
                participants = previewMemberListState.takeLast(1),
                isCameraEnabled = false,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `incoming call content with multiple participants`() {
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
    fun `incoming call content with multiple participants in dark mode`() {
        snapshot(isInDarkMode = true) {
            IncomingCallContent(
                call = previewCall,
                participants = previewMemberListState,
                isCameraEnabled = false,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `incoming call content with minimum parameters`() {
        snapshot {
            IncomingCallContent(
                call = previewCall,
            )
        }
    }

    @Test
    fun `incoming call content with minimum parameters in dark mode`() {
        snapshot(isInDarkMode = true) {
            IncomingCallContent(
                call = previewCall,
            )
        }
    }

    @Test
    fun `outgoing call details video`() {
        snapshotWithDarkMode {
            OutgoingCallDetails(participants = previewMemberListState)
        }
    }

    @Test
    fun `outgoing call details audio`() {
        snapshotWithDarkMode {
            OutgoingCallDetails(
                isVideoType = false,
                participants = previewMemberListState,
            )
        }
    }

    @Test
    fun `outgoing call options`() {
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
    fun `outgoing call content with one participant`() {
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
    fun `outgoing call content with one participant in dark mode`() {
        snapshot(isInDarkMode = true) {
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
    fun `outgoing call content with multiple participants`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                participants = previewMemberListState,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `outgoing call content with multiple participants in dark mode`() {
        snapshot(isInDarkMode = true) {
            OutgoingCallContent(
                call = previewCall,
                participants = previewMemberListState,
                onBackPressed = {},
            ) {}
        }
    }

    @Test
    fun `call content with multiple participants`() {
        snapshot {
            CallContent(call = previewCall)
        }
    }

    @Test
    fun `call content with multiple participants in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallContent(call = previewCall)
        }
    }

    @Test
    fun `outgoing call content with minimum parameters and video type`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                isVideoType = true,
            )
        }
    }

    @Test
    fun `outgoing call content with minimum parameters and video type in dark mode`() {
        snapshot(isInDarkMode = true) {
            OutgoingCallContent(
                call = previewCall,
                isVideoType = true,
            )
        }
    }

    @Test
    fun `outgoing call content with minimum parameters and audio type`() {
        snapshot {
            OutgoingCallContent(
                call = previewCall,
                isVideoType = false,
            )
        }
    }

    @Test
    fun `outgoing call content with minimum parameters and audio type in dark mode`() {
        snapshot(isInDarkMode = true) {
            OutgoingCallContent(
                call = previewCall,
                isVideoType = false,
            )
        }
    }
}
