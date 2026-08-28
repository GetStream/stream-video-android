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

package io.getstream.video.android.compose.theme

import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import io.getstream.video.android.compose.ui.MAX_PERCENT_DIFFERENCE
import io.getstream.video.android.compose.ui.PIXEL_4A_HDPI
import io.getstream.video.android.compose.ui.PaparazziComposeTest
import org.junit.Rule
import org.junit.Test

/**
 * Snapshots for the [VideoComponentFactory] default implementations that are not reached through
 * the component slots covered by the other snapshot tests.
 *
 * Some of these goldens are byte-identical to other goldens in the suite on purpose: the factory
 * defaults delegate to the same built-in components with the same preview data, and that identity
 * is exactly what these tests pin, together with the line and branch coverage of the default
 * implementations (for example the null fallbacks for `title`, `onCallAction` and `actions`).
 * Dark mode siblings also match their light counterparts until the light palette lands (see
 * AND-1419); the pairs are kept so they start diverging with no test changes, like the rest of
 * the suite.
 */
internal class VideoComponentFactoryTest : PaparazziComposeTest {

    @get:Rule
    override val paparazzi = Paparazzi(
        deviceConfig = PIXEL_4A_HDPI,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = MAX_PERCENT_DIFFERENCE,
    )

    @Test
    fun `factory call app bar with default title`() {
        snapshotWithDarkMode {
            VideoComponentFactoryCallAppBarPreview()
        }
    }

    @Test
    fun `factory control actions with default handler`() {
        snapshotWithDarkMode {
            VideoComponentFactoryControlActionsPreview()
        }
    }

    @Test
    fun `factory call lobby controls`() {
        snapshotWithDarkMode {
            VideoComponentFactoryLobbyControlsPreview()
        }
    }

    @Test
    fun `factory picture in picture content`() {
        snapshotWithDarkMode {
            VideoComponentFactoryPictureInPicturePreview()
        }
    }

    @Test
    fun `factory incoming call content`() {
        snapshot {
            VideoComponentFactoryIncomingCallPreview()
        }
    }

    @Test
    fun `factory incoming call content in dark mode`() {
        snapshot(isInDarkMode = true) {
            VideoComponentFactoryIncomingCallPreview()
        }
    }

    @Test
    fun `factory outgoing call content`() {
        snapshot {
            VideoComponentFactoryOutgoingCallPreview()
        }
    }

    @Test
    fun `factory outgoing call content in dark mode`() {
        snapshot(isInDarkMode = true) {
            VideoComponentFactoryOutgoingCallPreview()
        }
    }

    @Test
    fun `factory video moderation warning`() {
        snapshotWithDarkMode {
            VideoComponentFactoryModerationWarningPreview()
        }
    }

    @Test
    fun `factory participant video fallback`() {
        snapshotWithDarkMode {
            VideoComponentFactoryVideoFallbackPreview()
        }
    }

    @Test
    fun `factory empty defaults`() {
        snapshotWithDarkMode {
            VideoComponentFactoryEmptyDefaultsPreview()
        }
    }
}
