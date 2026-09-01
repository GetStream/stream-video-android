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
import io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContentPreview
import io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContentWithoutHeaderPreview
import io.getstream.video.android.compose.ui.components.call.activecall.AudioOnlyCallContentPreview
import io.getstream.video.android.compose.ui.components.call.activecall.AudioOnlyCallContentWithoutHeaderPreview
import org.junit.Rule
import org.junit.Test

internal class AudioCallContentTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun `audio call content`() {
        snapshot {
            AudioCallContentPreview()
        }
    }

    @Test
    fun `audio call content in dark mode`() {
        snapshot(isInDarkMode = true) {
            AudioCallContentPreview()
        }
    }

    @Test
    fun `audio call content without header`() {
        snapshot {
            AudioCallContentWithoutHeaderPreview()
        }
    }

    @Test
    fun `audio call content without header in dark mode`() {
        snapshot(isInDarkMode = true) {
            AudioCallContentWithoutHeaderPreview()
        }
    }

    @Test
    fun `audio only call content`() {
        snapshot {
            AudioOnlyCallContentPreview()
        }
    }

    @Test
    fun `audio only call content in dark mode`() {
        snapshot(isInDarkMode = true) {
            AudioOnlyCallContentPreview()
        }
    }

    @Test
    fun `audio only call content without header`() {
        snapshot {
            AudioOnlyCallContentWithoutHeaderPreview()
        }
    }

    @Test
    fun `audio only call content without header in dark mode`() {
        snapshot(isInDarkMode = true) {
            AudioOnlyCallContentWithoutHeaderPreview()
        }
    }
}
