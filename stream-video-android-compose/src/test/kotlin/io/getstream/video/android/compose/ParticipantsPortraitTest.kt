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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.internal.LazyColumnVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.internal.PortraitScreenSharingVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.internal.PortraitVideoRenderer
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantListAppBar
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsInfoActions
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsList
import io.getstream.video.android.compose.ui.components.participants.internal.InviteUserList
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockMemberStateList
import io.getstream.video.android.mock.mockParticipant
import io.getstream.video.android.mock.mockParticipantList
import org.junit.Rule
import org.junit.Test

internal class ParticipantsPortraitTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot ParticipantAvatars composable`() {
        snapshotWithDarkMode {
            ParticipantAvatars(participants = mockMemberStateList)
        }
    }

    @Test
    fun `snapshot ParticipantInformation composable`() {
        snapshotWithDarkMode {
            ParticipantInformation(
                callStatus = CallStatus.Incoming,
                participants = mockMemberStateList,
            )
        }
    }

    @Test
    fun `snapshot InviteUserList composable`() {
        snapshotWithDarkMode {
            InviteUserList(
                mockParticipantList.map { it.initialUser },
                onUserSelected = {},
                onUserUnSelected = {},
            )
        }
    }

    @Test
    fun `snapshot CallParticipantsInfoOptions composable`() {
        snapshotWithDarkMode {
            CallParticipantsInfoActions(
                isLocalAudioEnabled = false,
                onInviteUser = {},
                onMute = {},
            )
        }
    }

    @Test
    fun `snapshot CallParticipantsInfoAppBar composable`() {
        snapshotWithDarkMode {
            CallParticipantListAppBar(
                numberOfParticipants = 10,
                onBackPressed = {},
            )
        }
    }

    @Test
    fun `snapshot CallParticipant a local call composable`() {
        snapshot {
            ParticipantVideo(
                call = mockCall,
                participant = mockParticipantList[0],
                style = RegularVideoRendererStyle(isFocused = true),
            )
        }
    }

    @Test
    fun `snapshot CallParticipant a remote call composable`() {
        snapshot {
            ParticipantVideo(
                call = mockCall,
                participant = mockParticipantList[1],
                style = RegularVideoRendererStyle(isFocused = true),
            )
        }
    }

    @Test
    fun `snapshot ParticipantVideo composable`() {
        snapshot {
            ParticipantVideoRenderer(
                call = mockCall,
                participant = mockParticipant,
            ) {}
        }
    }

    @Test
    fun `snapshot LocalVideoContent composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            Box {
                FloatingParticipantVideo(
                    call = mockCall,
                    modifier = Modifier.fillMaxSize(),
                    participant = mockParticipant,
                    parentBounds = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot CallParticipantsList composable`() {
        snapshotWithDarkMode {
            CallParticipantsList(
                participants = mockParticipantList,
                onUserOptionsSelected = {},
                isLocalAudioEnabled = false,
                onInviteUser = {},
                onMute = {},
            ) {}
        }
    }

    @Test
    fun `snapshot PortraitParticipants1 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = mockParticipantList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            ) {
                PortraitVideoRenderer(
                    call = mockCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(1),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants2 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = mockParticipantList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            ) {
                PortraitVideoRenderer(
                    call = mockCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(2),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants3 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = mockParticipantList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            ) {
                PortraitVideoRenderer(
                    call = mockCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(3),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants4 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = mockParticipantList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            ) {
                PortraitVideoRenderer(
                    call = mockCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(4),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants5 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = mockParticipantList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            ) {
                PortraitVideoRenderer(
                    call = mockCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(5),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants6 composable`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            val participants = mockParticipantList

            Box(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            ) {
                PortraitVideoRenderer(
                    call = mockCall,
                    dominantSpeaker = participants[0],
                    callParticipants = participants.take(6),
                    modifier = Modifier.fillMaxSize(),
                    parentSize = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitScreenSharingContent for other participant composable`() {
        snapshot(isInDarkMode = true) {
            PortraitScreenSharingVideoRenderer(
                call = mockCall,
                session = ScreenSharingSession(participant = mockParticipantList[0]),
                participants = mockParticipantList,
                dominantSpeaker = mockParticipantList[1],
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `snapshot PortraitScreenSharingContent for myself composable`() {
        snapshot(isInDarkMode = true) {
            PortraitScreenSharingVideoRenderer(
                call = mockCall,
                session = ScreenSharingSession(participant = mockParticipantList[0]),
                participants = mockParticipantList,
                dominantSpeaker = mockParticipantList[0],
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun `snapshot ParticipantsColumn composable`() {
        snapshotWithDarkModeRow {
            LazyColumnVideoRenderer(
                call = mockCall,
                participants = mockParticipantList,
                dominantSpeaker = mockParticipant,
            )
        }
    }
}
