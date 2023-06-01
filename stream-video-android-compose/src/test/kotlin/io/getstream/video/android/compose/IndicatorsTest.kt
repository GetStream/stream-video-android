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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.model.Muted
import io.getstream.video.android.common.model.Speaking
import io.getstream.video.android.common.model.Unmuted
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.audio.ActiveSoundLevels
import io.getstream.video.android.compose.ui.components.audio.SoundIndicator
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantLabel
import io.getstream.video.android.compose.ui.components.connection.NetworkQualityIndicator
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.mock.mockParticipantList
import org.junit.Rule
import org.junit.Test

internal class IndicatorsTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot SoundIndicator composable`() {
        snapshot {
            Row {
                SoundIndicator(state = Speaking)
                SoundIndicator(state = Unmuted)
                SoundIndicator(state = Muted)
            }
        }
    }

    @Test
    fun `snapshot ActiveSoundLevels composable`() {
        snapshot {
            ActiveSoundLevels()
        }
    }

    @Test
    fun `snapshot Connection ConnectionQualityIndicator composable`() {
        snapshot {
            Row {
                NetworkQualityIndicator(
                    networkQuality = NetworkQuality.Poor()
                )
                NetworkQualityIndicator(
                    networkQuality = NetworkQuality.Good()
                )
                NetworkQualityIndicator(
                    networkQuality = NetworkQuality.Excellent()
                )
            }
        }
    }

    @Test
    fun `snapshot Connection ParticipantLabel composable`() {
        snapshot {
            Box {
                ParticipantLabel(
                    participant = mockParticipantList[1],
                    Alignment.BottomStart,
                )
            }
        }
    }
}
