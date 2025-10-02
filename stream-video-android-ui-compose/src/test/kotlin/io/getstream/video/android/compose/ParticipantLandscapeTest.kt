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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.internal.LandscapeScreenSharingVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.internal.LandscapeVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.internal.LazyRowVideoRenderer
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList
import org.junit.Rule
import org.junit.Test

internal class ParticipantLandscapeTest : BaseComposeTest() {

    @get:Rule
    val paparazziLandscape = Paparazzi(deviceConfig = DeviceConfig.PIXEL_2)

    override fun basePaparazzi(): Paparazzi = paparazziLandscape

    @Test
    fun `snapshot LandscapeParticipants1 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = previewParticipantsList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = previewCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(1),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants2 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = previewParticipantsList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = previewCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(2),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants3 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = previewParticipantsList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = previewCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(3),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants4 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = previewParticipantsList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = previewCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(4),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants5 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = previewParticipantsList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = previewCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(5),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants6 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = previewParticipantsList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
            ) {
                LandscapeVideoRenderer(
                    call = previewCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(6),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot LandscapeScreenSharingContent for other participant composable`() {
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
    fun `snapshot LandscapeScreenSharingContent for myself composable`() {
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
    fun `snapshot ParticipantsRow composable`() {
        snapshotWithDarkMode {
            LazyRowVideoRenderer(
                call = previewCall,
                participants = previewParticipantsList,
                dominantSpeaker = previewParticipant,
            )
        }
    }
}
