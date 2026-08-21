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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.MAX_PERCENT_DIFFERENCE
import io.getstream.video.android.compose.ui.PIXEL_2_LANDSCAPE_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewGridCall
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList
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
        val gridCall = previewGridCall(1)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 1 in dark mode`() {
        val gridCall = previewGridCall(1)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 2`() {
        val gridCall = previewGridCall(2)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 2 in dark mode`() {
        val gridCall = previewGridCall(2)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 3`() {
        val gridCall = previewGridCall(3)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 3 in dark mode`() {
        val gridCall = previewGridCall(3)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 4`() {
        val gridCall = previewGridCall(4)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 4 in dark mode`() {
        val gridCall = previewGridCall(4)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 5`() {
        val gridCall = previewGridCall(5)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 5 in dark mode`() {
        val gridCall = previewGridCall(5)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 6`() {
        val gridCall = previewGridCall(6)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 6 in dark mode`() {
        val gridCall = previewGridCall(6)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 7`() {
        val gridCall = previewGridCall(7)
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape participants 7 in dark mode`() {
        val gridCall = previewGridCall(7)
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = gridCall.call,
                    dominantSpeaker = gridCall.participants[0],
                    callParticipants = gridCall.participants,
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `landscape screen sharing content for other participant`() {
        snapshot {
            LandscapeScreenSharingVideoRenderer(
                call = previewCall,
                session = ScreenSharingSession(participant = previewParticipantsList[0]),
                participants = previewParticipantsList,
                dominantSpeaker = previewParticipant,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `landscape screen sharing content for other participant in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeScreenSharingVideoRenderer(
                call = previewCall,
                session = ScreenSharingSession(participant = previewParticipantsList[0]),
                participants = previewParticipantsList,
                dominantSpeaker = previewParticipant,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `landscape screen sharing content for myself`() {
        snapshot {
            LandscapeScreenSharingVideoRenderer(
                call = previewCall,
                session = ScreenSharingSession(participant = previewParticipantsList[0]),
                participants = previewParticipantsList,
                dominantSpeaker = previewParticipant,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `landscape screen sharing content for myself in dark mode`() {
        snapshot(isInDarkMode = true) {
            LandscapeScreenSharingVideoRenderer(
                call = previewCall,
                session = ScreenSharingSession(participant = previewParticipantsList[0]),
                participants = previewParticipantsList,
                dominantSpeaker = previewParticipant,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
