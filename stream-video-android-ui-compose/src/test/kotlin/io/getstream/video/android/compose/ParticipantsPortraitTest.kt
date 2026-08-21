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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import io.getstream.video.android.compose.ui.MAX_PERCENT_DIFFERENCE
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.compose.ui.components.call.renderer.FloatingParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideoRenderer
import io.getstream.video.android.compose.ui.components.call.renderer.RegularVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.internal.LazyColumnVideoRenderer
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantListAppBar
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsInfoActions
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsList
import io.getstream.video.android.compose.ui.components.participants.internal.InviteUserList
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewMemberListState
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList
import org.junit.Rule
import org.junit.Test

internal class ParticipantsPortraitTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = MAX_PERCENT_DIFFERENCE,
    )

    @Test
    fun `participant avatars`() {
        snapshotWithDarkMode {
            ParticipantAvatars(members = previewMemberListState)
        }
    }

    @Test
    fun `participant information`() {
        snapshotWithDarkMode {
            ParticipantInformation(
                callStatus = CallStatus.Incoming,
                members = previewMemberListState,
            )
        }
    }

    @Test
    fun `invite user list`() {
        snapshotWithDarkMode {
            InviteUserList(
                previewParticipantsList,
                onUserSelected = {},
                onUserUnSelected = {},
            )
        }
    }

    @Test
    fun `call participants info options`() {
        snapshotWithDarkMode {
            CallParticipantsInfoActions(
                isLocalAudioEnabled = false,
                onInviteUser = {},
                onMute = {},
            )
        }
    }

    @Test
    fun `call participants info app bar`() {
        snapshotWithDarkMode {
            CallParticipantListAppBar(
                numberOfParticipants = 10,
                onBackPressed = {},
            )
        }
    }

    @Test
    fun `call participant local`() {
        snapshot {
            ParticipantVideo(
                call = previewCall,
                participant = previewParticipantsList[0],
                style = RegularVideoRendererStyle(isFocused = true),
            )
        }
    }

    @Test
    fun `call participant local in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantVideo(
                call = previewCall,
                participant = previewParticipantsList[0],
                style = RegularVideoRendererStyle(isFocused = true),
            )
        }
    }

    @Test
    fun `call participant remote`() {
        snapshot {
            ParticipantVideo(
                call = previewCall,
                participant = previewParticipantsList[1],
                style = RegularVideoRendererStyle(isFocused = true),
            )
        }
    }

    @Test
    fun `call participant remote in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantVideo(
                call = previewCall,
                participant = previewParticipantsList[1],
                style = RegularVideoRendererStyle(isFocused = true),
            )
        }
    }

    @Test
    fun `participant video`() {
        snapshot {
            ParticipantVideoRenderer(
                call = previewCall,
                participant = previewParticipant,
            ) {}
        }
    }

    @Test
    fun `participant video in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantVideoRenderer(
                call = previewCall,
                participant = previewParticipant,
            ) {}
        }
    }

    @Test
    fun `local video content`() {
        snapshot {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            Box {
                FloatingParticipantVideo(
                    call = previewCall,
                    modifier = Modifier.fillMaxSize(),
                    participant = previewParticipant,
                    parentBounds = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `local video content in dark mode`() {
        snapshot(isInDarkMode = true) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp
            Box {
                FloatingParticipantVideo(
                    call = previewCall,
                    modifier = Modifier.fillMaxSize(),
                    participant = previewParticipant,
                    parentBounds = IntSize(screenWidth, screenHeight),
                )
            }
        }
    }

    @Test
    fun `call participants list`() {
        snapshotWithDarkMode {
            CallParticipantsList(
                participants = previewParticipantsList,
                onUserOptionsSelected = {},
                isLocalAudioEnabled = false,
                onInviteUser = {},
                onMute = {},
            ) {}
        }
    }

    @Test
    fun `participants column`() {
        snapshotWithDarkModeRow {
            LazyColumnVideoRenderer(
                call = previewCall,
                participants = previewParticipantsList,
                dominantSpeaker = previewParticipant,
            )
        }
    }
}
