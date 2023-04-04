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

import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.compose.ui.components.call.controls.internal.LandscapeCallControls
import io.getstream.video.android.compose.ui.components.call.controls.internal.RegularCallControls
import io.getstream.video.android.compose.ui.components.connection.ConnectionQualityIndicator
import io.getstream.video.android.core.call.state.CallMediaState
import org.junit.Rule
import org.junit.Test
import stream.video.sfu.models.ConnectionQuality

internal class CallComponentsTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun `snapshot CallAppBar composable`() {
        paparazzi.snapshot {
            VideoTheme {
                CallAppBar()
            }
        }
    }

    @Test
    fun `snapshot RegularCallControls composable`() {
        paparazzi.snapshot {
            VideoTheme {
                RegularCallControls(
                    callMediaState = CallMediaState(),
                    isScreenSharing = false
                ) {}
            }
        }
    }

    @Test
    fun `snapshot LandscapeCallControls composable`() {
        paparazzi.snapshot {
            VideoTheme {
                LandscapeCallControls(
                    callMediaState = CallMediaState(),
                    isScreenSharing = false
                ) {}
            }
        }
    }

    @Test
    fun `snapshot CallControls composable`() {
        paparazzi.snapshot {
            VideoTheme {
                CallControls(
                    callMediaState = CallMediaState(),
                    isScreenSharing = false
                ) {}
            }
        }
    }

    @Test
    fun `snapshot Connection ConnectionQualityIndicator composable`() {
        paparazzi.snapshot {
            VideoTheme {
                ConnectionQualityIndicator(
                    connectionQuality = ConnectionQuality.CONNECTION_QUALITY_GOOD
                )
            }
        }
    }
}
