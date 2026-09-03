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

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.compose.ui.components.call.renderer.CallParticipantLocalPreview
import io.getstream.video.android.compose.ui.components.call.renderer.CallParticipantRemotePreview
import io.getstream.video.android.compose.ui.components.call.renderer.LocalVideoContentPreview
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideoPreview
import io.getstream.video.android.compose.ui.components.call.renderer.internal.ParticipantsColumnPreview
import io.getstream.video.android.compose.ui.components.participants.ParticipantAvatarsPreview
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantListAppBarPreview
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsInfoActionsPreview
import io.getstream.video.android.compose.ui.components.participants.internal.CallParticipantsListPreview
import io.getstream.video.android.compose.ui.components.participants.internal.InviteUserListPreview
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformationPreview
import org.junit.Rule
import org.junit.Test

internal class ParticipantsPortraitTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun `participant avatars`() {
        snapshotWithDarkMode {
            ParticipantAvatarsPreview()
        }
    }

    @Test
    fun `participant information`() {
        snapshotWithDarkMode {
            ParticipantInformationPreview()
        }
    }

    @Test
    fun `invite user list`() {
        snapshotWithDarkMode {
            InviteUserListPreview()
        }
    }

    @Test
    fun `call participants info options`() {
        snapshotWithDarkMode {
            CallParticipantsInfoActionsPreview()
        }
    }

    @Test
    fun `call participants info app bar`() {
        snapshotWithDarkMode {
            CallParticipantListAppBarPreview()
        }
    }

    @Test
    fun `call participant local`() {
        snapshot {
            CallParticipantLocalPreview()
        }
    }

    @Test
    fun `call participant local in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallParticipantLocalPreview()
        }
    }

    @Test
    fun `call participant remote`() {
        snapshot {
            CallParticipantRemotePreview()
        }
    }

    @Test
    fun `call participant remote in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallParticipantRemotePreview()
        }
    }

    @Test
    fun `participant video`() {
        snapshot {
            ParticipantVideoPreview()
        }
    }

    @Test
    fun `participant video in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantVideoPreview()
        }
    }

    @Test
    fun `local video content`() {
        snapshot {
            LocalVideoContentPreview()
        }
    }

    @Test
    fun `local video content in dark mode`() {
        snapshot(isInDarkMode = true) {
            LocalVideoContentPreview()
        }
    }

    @Test
    fun `call participants list`() {
        snapshot {
            CallParticipantsListPreview()
        }
    }

    @Test
    fun `call participants list in dark mode`() {
        snapshot(isInDarkMode = true) {
            CallParticipantsListPreview()
        }
    }

    @Test
    fun `participants column`() {
        snapshotWithDarkModeRow {
            ParticipantsColumnPreview()
        }
    }
}
