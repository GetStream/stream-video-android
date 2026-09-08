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
import io.getstream.video.android.compose.ui.PIXEL_2_LANDSCAPE_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import org.junit.Rule
import org.junit.Test

internal class LandscapeVideoRendererTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_2_LANDSCAPE_HDPI,
        maxPercentDifference = MAX_PERCENT_DIFFERENCE,
    )

    @Test
    fun `landscape participants 1`() {
        snapshot {
            LandscapeParticipantsPreview1()
        }
    }

    @Test
    fun `landscape participants 1 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview1()
        }
    }

    @Test
    fun `landscape participants 2`() {
        snapshot {
            LandscapeParticipantsPreview2()
        }
    }

    @Test
    fun `landscape participants 2 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview2()
        }
    }

    @Test
    fun `landscape participants 3`() {
        snapshot {
            LandscapeParticipantsPreview3()
        }
    }

    @Test
    fun `landscape participants 3 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview3()
        }
    }

    @Test
    fun `landscape participants 4`() {
        snapshot {
            LandscapeParticipantsPreview4()
        }
    }

    @Test
    fun `landscape participants 4 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview4()
        }
    }

    @Test
    fun `landscape participants 5`() {
        snapshot {
            LandscapeParticipantsPreview5()
        }
    }

    @Test
    fun `landscape participants 5 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview5()
        }
    }

    @Test
    fun `landscape participants 6`() {
        snapshot {
            LandscapeParticipantsPreview6()
        }
    }

    @Test
    fun `landscape participants 6 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview6()
        }
    }

    @Test
    fun `landscape participants 7`() {
        snapshot {
            LandscapeParticipantsPreview7()
        }
    }

    @Test
    fun `landscape participants 7 in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeParticipantsPreview7()
        }
    }

    @Test
    fun `landscape screen sharing content for other participant`() {
        snapshot {
            LandscapeScreenSharingContentPreview()
        }
    }

    @Test
    fun `landscape screen sharing content for other participant in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeScreenSharingContentPreview()
        }
    }

    @Test
    fun `landscape screen sharing content for myself`() {
        snapshot {
            LandscapeScreenSharingMyContentPreview()
        }
    }

    @Test
    fun `landscape screen sharing content for myself in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeScreenSharingMyContentPreview()
        }
    }
}
