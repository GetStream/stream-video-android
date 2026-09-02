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
import io.getstream.video.android.compose.ui.MAX_PERCENT_DIFFERENCE
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import io.getstream.video.android.compose.ui.components.base.BadgesWithButtonsPreview
import io.getstream.video.android.compose.ui.components.base.ButtonWithIconsPreview
import io.getstream.video.android.compose.ui.components.base.DifferentSizeButtonsPreview
import io.getstream.video.android.compose.ui.components.base.GenericContainerPreview
import io.getstream.video.android.compose.ui.components.base.InputFieldsPreview
import io.getstream.video.android.compose.ui.components.base.RegularButtonsPreview
import io.getstream.video.android.compose.ui.components.base.RegularIconButtonsPreview
import io.getstream.video.android.compose.ui.components.base.ShowProgressIntoIconButtonsPreview
import io.getstream.video.android.compose.ui.components.base.ToggleButtonsPreview
import io.getstream.video.android.compose.ui.components.base.ToggleIconButtonsPreview
import org.junit.Rule
import org.junit.Test

internal class BaseComponentsTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = MAX_PERCENT_DIFFERENCE,
    )

    @Test
    fun `regular icon buttons`() {
        snapshotWithDarkMode {
            RegularIconButtonsPreview()
        }
    }

    @Test
    fun `regular buttons`() {
        snapshot {
            RegularButtonsPreview()
        }
    }

    @Test
    fun `regular buttons in dark mode`() {
        snapshot(isInDarkMode = true) {
            RegularButtonsPreview()
        }
    }

    @Test
    fun `button with icons`() {
        snapshotWithDarkMode {
            ButtonWithIconsPreview()
        }
    }

    @Test
    fun `different size buttons`() {
        snapshotWithDarkMode {
            DifferentSizeButtonsPreview()
        }
    }

    @Test
    fun `toggle icon buttons`() {
        snapshotWithDarkMode {
            ToggleIconButtonsPreview()
        }
    }

    @Test
    fun `toggle buttons`() {
        snapshotWithDarkMode {
            ToggleButtonsPreview()
        }
    }

    @Test
    fun `show progress into icon buttons`() {
        snapshotWithDarkMode {
            ShowProgressIntoIconButtonsPreview()
        }
    }

    @Test
    fun `input fields`() {
        snapshot {
            InputFieldsPreview()
        }
    }

    @Test
    fun `input fields in dark mode`() {
        snapshot(isInDarkMode = true) {
            InputFieldsPreview()
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
}
