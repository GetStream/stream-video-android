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

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.compose.ui.components.call.activecall.CallContentDeprecatedOverloadPreview
import io.getstream.video.android.compose.ui.components.call.activecall.CallContentMultipleParticipantsPreview
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContentMinimumPreview
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContentMultipleParticipantsPreview
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContentOneParticipantPreview
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallControlsPreview
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallDetailsAudioPreview
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContentMinimumVideoPreview
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContentMultipleParticipantsPreview
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContentOneParticipantPreview
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallControlsPreview
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallDetailsVideoPreview
import org.junit.Rule
import org.junit.Test

internal class CallContentTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun `incoming call details audio`() {
        snapshotWithDarkMode {
            IncomingCallDetailsAudioPreview()
        }
    }

    @Test
    fun `incoming call options`() {
        snapshotWithDarkMode {
            IncomingCallControlsPreview()
        }
    }

    @Test
    fun `incoming call content with one participant`() {
        snapshot {
            IncomingCallContentOneParticipantPreview()
        }
    }

    @Test
    fun `incoming call content with one participant in dark mode`() {
        snapshot(isInDarkMode = true) {
            IncomingCallContentOneParticipantPreview()
        }
    }

    @Test
    fun `incoming call content with multiple participants`() {
        snapshot {
            IncomingCallContentMultipleParticipantsPreview()
        }
    }

    @Test
    fun `incoming call content with multiple participants in dark mode`() {
        snapshot(isInDarkMode = true) {
            IncomingCallContentMultipleParticipantsPreview()
        }
    }

    @Test
    fun `incoming call content with minimum parameters`() {
        snapshot {
            IncomingCallContentMinimumPreview()
        }
    }

    @Test
    fun `incoming call content with minimum parameters in dark mode`() {
        snapshot(isInDarkMode = true) {
            IncomingCallContentMinimumPreview()
        }
    }

    @Test
    fun `outgoing call details video`() {
        snapshotWithDarkMode {
            OutgoingCallDetailsVideoPreview()
        }
    }

    @Test
    fun `outgoing call options`() {
        snapshotWithDarkMode {
            OutgoingCallControlsPreview()
        }
    }

    @Test
    fun `outgoing call content with one participant`() {
        snapshot {
            OutgoingCallContentOneParticipantPreview()
        }
    }

    @Test
    fun `outgoing call content with one participant in dark mode`() {
        snapshot(isInDarkMode = true) {
            OutgoingCallContentOneParticipantPreview()
        }
    }

    @Test
    fun `outgoing call content with multiple participants`() {
        snapshot {
            OutgoingCallContentMultipleParticipantsPreview()
        }
    }

    @Test
    fun `outgoing call content with multiple participants in dark mode`() {
        snapshot(isInDarkMode = true) {
            OutgoingCallContentMultipleParticipantsPreview()
        }
    }

    @Test
    fun `call content with multiple participants`() {
        snapshot {
            CallContentMultipleParticipantsPreview()
        }
    }

    @Test
    fun `call content with multiple participants in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallContentMultipleParticipantsPreview()
        }
    }

    @Test
    fun `call content deprecated overload`() {
        snapshot {
            CallContentDeprecatedOverloadPreview()
        }
    }

    @Test
    fun `call content deprecated overload in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallContentDeprecatedOverloadPreview()
        }
    }

    @Test
    fun `outgoing call content with minimum parameters and video type`() {
        snapshot {
            OutgoingCallContentMinimumVideoPreview()
        }
    }

    @Test
    fun `outgoing call content with minimum parameters and video type in dark mode`() {
        snapshot(isInDarkMode = true) {
            OutgoingCallContentMinimumVideoPreview()
        }
    }
}
