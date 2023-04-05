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
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.common.util.mockParticipant
import io.getstream.video.android.common.util.mockParticipantList
import io.getstream.video.android.common.util.mockVideoTrack
import io.getstream.video.android.compose.state.ui.internal.InviteUserItemState
import io.getstream.video.android.compose.state.ui.internal.ParticipantList
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.compose.ui.components.participants.LocalVideoContent
import io.getstream.video.android.compose.ui.components.participants.ParticipantVideo
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsInfoAppBar
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsInfoOptions
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsList
import io.getstream.video.android.compose.ui.components.participants.internal.InviteUserList
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantsColumn
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantsRow
import io.getstream.video.android.compose.ui.components.participants.internal.PortraitParticipants
import io.getstream.video.android.compose.ui.components.participants.internal.PortraitScreenSharingContent
import io.getstream.video.android.compose.ui.components.participants.internal.ScreenSharingCallParticipantsContent
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.core.model.toUser
import org.junit.Rule
import org.junit.Test

internal class ParticipantsPortraitTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot ParticipantAvatars composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantAvatars(participants = mockParticipantList)
            }
        }
    }

    @Test
    fun `snapshot ParticipantInformation composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantInformation(
                    callStatus = CallStatus.Incoming,
                    participants = mockParticipantList.map {
                        CallUser(
                            id = it.id,
                            name = it.name,
                            role = it.role,
                            state = null,
                            imageUrl = it.profileImageURL ?: "",
                            createdAt = null,
                            updatedAt = null,
                            teams = emptyList()
                        )
                    }
                )
            }
        }
    }

    @Test
    fun `snapshot InviteUserList composable`() {
        paparazzi.snapshot {
            VideoTheme {
                InviteUserList(
                    mockParticipantList.map { InviteUserItemState(it.toUser()) },
                    onUserSelected = {}
                )
            }
        }
    }

    @Test
    fun `snapshot CallParticipantsInfoOptions composable`() {
        paparazzi.snapshot {
            VideoTheme {
                CallParticipantsInfoOptions(
                    isCurrentUserMuted = false,
                    onOptionSelected = { }
                )
            }
        }
    }

    @Test
    fun `snapshot CallParticipantsInfoAppBar composable`() {
        paparazzi.snapshot {
            VideoTheme {
                CallParticipantsInfoAppBar(
                    numberOfParticipants = 10,
                    infoStateMode = ParticipantList,
                    onBackPressed = {}
                ) {}
            }
        }
    }

    @Test
    fun `snapshot CallParticipant composable`() {
        paparazzi.snapshot {
            VideoTheme {
                CallParticipant(
                    call = null,
                    participant = mockParticipant
                )
            }
        }
    }

    @Test
    fun `snapshot ParticipantVideo composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantVideo(
                    call = null,
                    participant = mockParticipant
                ) {}
            }
        }
    }

    @Test
    fun `snapshot LocalVideoContent composable`() {
        paparazzi.snapshot {
            VideoTheme {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp
                LocalVideoContent(
                    call = null,
                    modifier = Modifier.fillMaxSize(),
                    localParticipant = mockParticipant,
                    parentBounds = IntSize(screenWidth, screenHeight),
                    paddingValues = PaddingValues(0.dp)
                )
            }
        }
    }

    @Test
    fun `snapshot CallParticipantsList composable`() {
        paparazzi.snapshot {
            VideoTheme {
                CallParticipantsList(
                    participantsState = mockParticipantList,
                    onUserOptionsSelected = {}
                )
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants1 composable`() {
        paparazzi.snapshot {
            VideoTheme {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp

                Box(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground)
                ) {
                    PortraitParticipants(
                        call = null,
                        primarySpeaker = mockParticipantList[0],
                        callParticipants = mockParticipantList.take(1),
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = PaddingValues(0.dp),
                        parentSize = IntSize(screenWidth, screenHeight)
                    ) {}
                }
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants2 composable`() {
        paparazzi.snapshot {
            VideoTheme {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp

                Box(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground)
                ) {
                    PortraitParticipants(
                        call = null,
                        primarySpeaker = mockParticipantList[0],
                        callParticipants = mockParticipantList.take(2),
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = PaddingValues(0.dp),
                        parentSize = IntSize(screenWidth, screenHeight)
                    ) {}
                }
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants3 composable`() {
        paparazzi.snapshot {
            VideoTheme {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp

                Box(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground)
                ) {
                    PortraitParticipants(
                        call = null,
                        primarySpeaker = mockParticipantList[0],
                        callParticipants = mockParticipantList.take(3),
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = PaddingValues(0.dp),
                        parentSize = IntSize(screenWidth, screenHeight)
                    ) {}
                }
            }
        }
    }

    @Test
    fun `snapshot PortraitParticipants4 composable`() {
        paparazzi.snapshot {
            VideoTheme {
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp

                Box(
                    modifier = Modifier.background(color = VideoTheme.colors.appBackground)
                ) {
                    PortraitParticipants(
                        call = null,
                        primarySpeaker = mockParticipantList[0],
                        callParticipants = mockParticipantList.take(4),
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = PaddingValues(0.dp),
                        parentSize = IntSize(screenWidth, screenHeight)
                    ) {}
                }
            }
        }
    }

    @Test
    fun `snapshot PortraitScreenSharingContent composable`() {
        paparazzi.snapshot {
            VideoTheme {
                PortraitScreenSharingContent(
                    call = null,
                    session = ScreenSharingSession(
                        track = mockParticipantList.first().videoTrack ?: mockVideoTrack,
                        participant = mockParticipantList.first()
                    ),
                    participants = mockParticipantList,
                    paddingValues = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxSize(),
                    onRender = {}
                ) {}
            }
        }
    }

    @Test
    fun `snapshot ScreenSharingCallParticipantsContent composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ScreenSharingCallParticipantsContent(
                    call = null,
                    session = ScreenSharingSession(
                        track = mockParticipantList.first().videoTrack ?: mockVideoTrack,
                        participant = mockParticipantList.first()
                    ),
                    participants = mockParticipantList,
                    onCallAction = {},
                    modifier = Modifier.fillMaxSize(),
                    callMediaState = CallMediaState()
                )
            }
        }
    }

    @Test
    fun `snapshot ParticipantsRow composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantsRow(
                    call = null,
                    participants = mockParticipantList
                )
            }
        }
    }

    @Test
    fun `snapshot ParticipantsColumn composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ParticipantsColumn(
                    call = null,
                    participants = mockParticipantList
                )
            }
        }
    }
}
