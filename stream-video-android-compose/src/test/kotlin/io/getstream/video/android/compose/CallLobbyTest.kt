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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.call.lobby.CallLobby
import io.getstream.video.android.mock.mockCall
import org.junit.Rule
import org.junit.Test

internal class CallLobbyTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot CallLobby composable`() {
        snapshot {
            CallLobby(
                modifier = Modifier.fillMaxWidth(),
                call = mockCall,
            )
        }
    }
}
