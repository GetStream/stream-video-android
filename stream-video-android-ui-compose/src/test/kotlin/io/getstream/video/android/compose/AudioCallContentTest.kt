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

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent
import io.getstream.video.android.compose.ui.components.call.activecall.AudioOnlyCallContent
import io.getstream.video.android.mock.previewCall
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

internal class AudioCallContentTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot AudioCallContent in default state`() {
        snapshot {
            AudioCallContent(
                call = previewCall,
                isMicrophoneEnabled = false,
                durationPlaceholder = "11:45",
            )
        }
    }

    @Test
    fun `snapshot AudioCallContent without header`() {
        snapshot {
            AudioCallContent(
                call = previewCall,
                isMicrophoneEnabled = false,
                isShowingHeader = false,
            )
        }
    }

    @Ignore("https://linear.app/stream/issue/AND-786/fix-video-snapshot-tests")
    @Test
    fun `snapshot AudioOnlyCallContent in default state`() {
        snapshot {
            AudioOnlyCallContent(
                call = previewCall,
                isMicrophoneEnabled = false,
                durationPlaceholder = "11:45",
            )
        }
    }

    @Ignore("https://linear.app/stream/issue/AND-786/fix-video-snapshot-tests")
    @Test
    fun `snapshot AudioOnlyCallContent without header`() {
        snapshot {
            AudioOnlyCallContent(
                call = previewCall,
                isMicrophoneEnabled = false,
                isShowingHeader = false,
            )
        }
    }
}
