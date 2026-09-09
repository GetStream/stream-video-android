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
import io.getstream.video.android.compose.ui.components.base.BadgesWithButtonsPreview
import io.getstream.video.android.compose.ui.components.base.GenericContainerPreview
import io.getstream.video.android.compose.ui.components.base.StreamButtonSizesPreview
import io.getstream.video.android.compose.ui.components.base.StreamButtonStylesPreview
import io.getstream.video.android.compose.ui.components.base.StreamListItemsPreview
import io.getstream.video.android.compose.ui.components.base.StreamScrimPreview
import io.getstream.video.android.compose.ui.components.base.StreamTextFieldPreview
import org.junit.Rule
import org.junit.Test

internal class BaseComponentsTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun `button styles enabled`() {
        snapshotWithDarkMode {
            StreamButtonStylesPreview(enabled = true)
        }
    }

    @Test
    fun `button styles disabled`() {
        snapshotWithDarkMode {
            StreamButtonStylesPreview(enabled = false)
        }
    }

    @Test
    fun `button sizes`() {
        snapshotWithDarkMode {
            StreamButtonSizesPreview()
        }
    }

    @Test
    fun `text fields`() {
        snapshot {
            StreamTextFieldPreview()
        }
    }

    @Test
    fun `text fields in dark mode`() {
        snapshot(isInDarkMode = true) {
            StreamTextFieldPreview()
        }
    }

    @Test
    fun `generic container`() {
        snapshotWithDarkMode {
            GenericContainerPreview()
        }
    }

    @Test
    fun `badges with buttons`() {
        snapshotWithDarkMode {
            BadgesWithButtonsPreview()
        }
    }

    @Test
    fun `list items`() {
        snapshotWithDarkMode {
            StreamListItemsPreview()
        }
    }

    @Test
    fun `scrim`() {
        snapshotWithDarkMode {
            StreamScrimPreview()
        }
    }
}
