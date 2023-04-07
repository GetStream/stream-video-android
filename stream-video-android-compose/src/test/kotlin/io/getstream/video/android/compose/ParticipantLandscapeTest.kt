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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.mockParticipant
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.common.util.mockVideoTrack
import io.getstream.video.android.compose.base.BaseLandscapeComposeTest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.internal.DefaultCallControlsContent
import io.getstream.video.android.compose.ui.components.participants.internal.LandscapeParticipants
import io.getstream.video.android.compose.ui.components.participants.internal.LandscapeScreenSharingContent
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.model.ScreenSharingSession
import org.junit.Test

internal class ParticipantLandscapeTest : BaseLandscapeComposeTest() {

    @Test
    fun `snapshot LandscapeParticipants1 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground)
            ) {
                LandscapeParticipants(
                    call = null,
                    primarySpeaker = mockParticipant,
                    callParticipants = mockParticipantList.take(1),
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = PaddingValues(0.dp),
                    parentSize = IntSize(screenWidth, screenHeight)
                ) {}
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants2 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground)
            ) {
                LandscapeParticipants(
                    call = null,
                    primarySpeaker = mockParticipant,
                    callParticipants = mockParticipantList.take(2),
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = PaddingValues(0.dp),
                    parentSize = IntSize(screenWidth, screenHeight)
                ) {}
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants3 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground)
            ) {
                LandscapeParticipants(
                    call = null,
                    primarySpeaker = mockParticipant,
                    callParticipants = mockParticipantList.take(3),
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = PaddingValues(0.dp),
                    parentSize = IntSize(screenWidth, screenHeight)
                ) {}
            }
        }
    }

    @Test
    fun `snapshot LandscapeParticipants4 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground)
            ) {
                LandscapeParticipants(
                    call = null,
                    primarySpeaker = mockParticipant,
                    callParticipants = mockParticipantList.take(4),
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = PaddingValues(0.dp),
                    parentSize = IntSize(screenWidth, screenHeight)
                ) {}
            }
        }
    }

    @Test
    fun `snapshot LandscapeScreenSharingContent composable`() {
        snapshot {
            LandscapeScreenSharingContent(
                call = null,
                session = ScreenSharingSession(
                    track = mockParticipantList.first().videoTrack ?: mockVideoTrack,
                    participant = mockParticipantList.first()
                ),
                participants = mockParticipantList,
                paddingValues = PaddingValues(0.dp),
                modifier = Modifier.fillMaxSize(),
                isFullscreen = true,
                onRender = {},
                onCallAction = {},
                onBackPressed = {}
            ) {
                DefaultCallControlsContent(
                    call = null,
                    callMediaState = CallMediaState(),
                ) {}
            }
        }
    }
}
