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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.ui.MAX_PERCENT_DIFFERENCE
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import org.junit.Rule
import org.junit.Test

internal class PortraitVideoRendererTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        maxPercentDifference = MAX_PERCENT_DIFFERENCE,
    )

    @Test
    fun `portrait participants 1`() {
        snapshot {
            PortraitParticipantsPreview1()
        }
    }

    @Test
    fun `portrait participants 1 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview1()
        }
    }

    @Test
    fun `portrait participants 2`() {
        snapshot {
            PortraitParticipantsPreview2()
        }
    }

    @Test
    fun `portrait participants 2 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview2()
        }
    }

    @Test
    fun `portrait participants 3`() {
        snapshot {
            PortraitParticipantsPreview3()
        }
    }

    @Test
    fun `portrait participants 3 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview3()
        }
    }

    @Test
    fun `portrait participants 4`() {
        snapshot {
            PortraitParticipantsPreview4()
        }
    }

    @Test
    fun `portrait participants 4 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview4()
        }
    }

    @Test
    fun `portrait participants 5`() {
        snapshot {
            PortraitParticipantsPreview5()
        }
    }

    @Test
    fun `portrait participants 5 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview5()
        }
    }

    @Test
    fun `portrait participants 6`() {
        snapshot {
            PortraitParticipantsPreview6()
        }
    }

    @Test
    fun `portrait participants 6 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview6()
        }
    }

    @Test
    fun `portrait participants 7`() {
        snapshot {
            PortraitParticipantsPreview7()
        }
    }

    @Test
    fun `portrait participants 7 in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitParticipantsPreview7()
        }
    }

    @Test
    fun `portrait screen sharing content for other participant`() {
        snapshot {
            PortraitScreenSharingContentPreview()
        }
    }

    @Test
    fun `portrait screen sharing content for other participant in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitScreenSharingContentPreview()
        }
    }

    @Test
    fun `portrait screen sharing content for myself`() {
        snapshot {
            PortraitScreenSharingMyContentPreview()
        }
    }

    @Test
    fun `portrait screen sharing content for myself in dark mode`() {
        snapshot(isInDarkMode = true) {
            PortraitScreenSharingMyContentPreview()
        }
    }
}
