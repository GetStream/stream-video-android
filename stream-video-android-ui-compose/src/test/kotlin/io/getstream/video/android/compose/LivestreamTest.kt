/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import androidx.compose.foundation.layout.Box
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayerOverlay
import io.getstream.video.android.mock.previewCall
import org.junit.Rule
import org.junit.Test

internal class LivestreamTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_2.copy(orientation = ScreenOrientation.LANDSCAPE),
    )

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot Livestream Player Overlay composable`() {
        snapshot {
            Box {
                LivestreamPlayerOverlay(call = previewCall)
            }
        }
    }

    @Test
    fun `snapshot Livestream Player composable`() {
        snapshot {
            LivestreamPlayer(call = previewCall)
        }
    }
}
