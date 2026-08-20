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

package io.getstream.video.android.compose.ui.components.call.pinning

import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import org.junit.Rule
import org.junit.Test

internal class ParticipantActionsTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(deviceConfig = PIXEL_4A_HDPI)

    @Test
    fun `participant actions dialog`() {
        snapshot {
            ParticipantActionDialogPreview()
        }
    }

    @Test
    fun `participant actions dialog in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantActionDialogPreview()
        }
    }

    @Test
    fun `participant actions`() {
        snapshot {
            ParticipantActionsPreview()
        }
    }

    @Test
    fun `participant actions in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantActionsPreview()
        }
    }

    @Test
    fun `participant actions kick`() {
        snapshot {
            ParticipantActionsKickPreview()
        }
    }

    @Test
    fun `participant actions kick in dark mode`() {
        snapshot(isInDarkMode = true) {
            ParticipantActionsKickPreview()
        }
    }
}
