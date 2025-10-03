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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.audio.AudioAppBar
import io.getstream.video.android.compose.ui.components.audio.AudioControlActions
import io.getstream.video.android.compose.ui.components.audio.AudioParticipantsGrid
import io.getstream.video.android.compose.ui.components.audio.AudioRoomContent
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

internal class AudioRoomTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_2.copy(orientation = ScreenOrientation.LANDSCAPE),
    )

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
            AudioControlActions(call = previewCall, modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `snapshot AudioParticipantsGrid composable`() {
        snapshot {
            AudioParticipantsGrid(
                modifier = Modifier.fillMaxSize(),
                participants = previewParticipantsList,
            )
        }
    }

    @Ignore("https://linear.app/stream/issue/AND-786/fix-video-snapshot-tests")
    @Test
    fun `snapshot AudioRoom composable`() {
        snapshot {
            AudioRoomContent(
                modifier = Modifier.fillMaxSize(),
                call = previewCall,
            )
        }
    }

    @Ignore("https://linear.app/stream/issue/AND-786/fix-video-snapshot-tests")
    @Test
    fun `snapshot AudioRoom DarkMode composable`() {
        snapshot(isInDarkMode = true) {
            AudioRoomContent(
                modifier = Modifier.fillMaxSize(),
                call = previewCall,
            )
        }
    }
}
