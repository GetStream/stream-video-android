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
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobbyDeprecatedOverloadPreview
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobbyMicAndCameraIssuePreview
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobbyMicAndCameraOffPreview
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobbyPreview
import org.junit.Rule
import org.junit.Test

/**
 * The lobby is taller than half of the device, so light and dark mode are recorded as separate
 * goldens instead of the two-half layout.
 */
internal class CallLobbyTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun `call lobby`() {
        snapshot {
            CallLobbyPreview()
        }
    }

    @Test
    fun `call lobby in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallLobbyPreview()
        }
    }

    @Test
    fun `call lobby with mic and camera off`() {
        snapshot {
            CallLobbyMicAndCameraOffPreview()
        }
    }

    @Test
    fun `call lobby with mic and camera off in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallLobbyMicAndCameraOffPreview()
        }
    }

    @Test
    fun `call lobby with mic and camera issue`() {
        snapshot {
            CallLobbyMicAndCameraIssuePreview()
        }
    }

    @Test
    fun `call lobby with mic and camera issue in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallLobbyMicAndCameraIssuePreview()
        }
    }

    @Test
    fun `call lobby deprecated overload`() {
        snapshot {
            CallLobbyDeprecatedOverloadPreview()
        }
    }

    @Test
    fun `call lobby deprecated overload in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallLobbyDeprecatedOverloadPreview()
        }
    }
}
