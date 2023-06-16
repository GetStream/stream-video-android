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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.audio.AudioAppBar
import io.getstream.video.android.compose.ui.components.audio.AudioControlActions
import io.getstream.video.android.compose.ui.components.audio.AudioParticipantsGrid
import io.getstream.video.android.compose.ui.components.audio.AudioRoom
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList
import org.junit.Rule
import org.junit.Test

internal class AudioRoomTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.NEXUS_5_LAND)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot AudioAppBar composable`() {
        snapshotWithDarkMode {
            AudioAppBar(title = "Audio Room Number 05")
        }
    }

    @Test
    fun `snapshot AudioControlActions composable`() {
        snapshot {
            AudioControlActions(call = mockCall, modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `snapshot AudioParticipantsGrid composable`() {
        snapshot {
            AudioParticipantsGrid(
                modifier = Modifier.fillMaxSize(),
                participants = mockParticipantList
            )
        }
    }

    @Test
    fun `snapshot AudioRoom composable`() {
        snapshot {
            AudioRoom(
                modifier = Modifier.fillMaxSize(),
                call = mockCall
            )
        }
    }

    @Test
    fun `snapshot AudioRoom DarkMode composable`() {
        snapshot(isInDarkMode = true) {
            AudioRoom(
                modifier = Modifier.fillMaxSize(),
                call = mockCall
            )
        }
    }
}
