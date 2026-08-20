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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobby
import io.getstream.video.android.mock.previewCall
import org.junit.Rule
import org.junit.Test

internal class CallLobbyTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(deviceConfig = PIXEL_4A_HDPI)

    @Test
    fun `snapshot CallLobby composable`() {
        snapshot {
            CallLobby(
                modifier = Modifier.fillMaxWidth(),
                call = previewCall,
            )
        }
    }

    @Test
    fun `snapshot CallLobby composable with camera disabled`() {
        snapshot {
            CallLobby(
                modifier = Modifier.fillMaxWidth(),
                call = previewCall,
                isCameraEnabled = false,
            )
        }
    }

    @Test
    fun `snapshot CallLobby composable dark mode`() {
        snapshot(isInDarkMode = true) {
            CallLobby(
                modifier = Modifier.fillMaxWidth(),
                call = previewCall,
            )
        }
    }
}
