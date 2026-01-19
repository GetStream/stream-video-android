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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.getstream.video.android.compose.base.BaseComposeTest
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.AcceptCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ChatDialogAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.mock.previewCall
import org.junit.Rule
import org.junit.Test

internal class CallControlsTest : BaseComposeTest() {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    override fun basePaparazzi(): Paparazzi = paparazzi

    @Test
    fun `snapshot CallControls composable`() {
        snapshotWithDarkMode {
            ControlActions(call = previewCall, onCallAction = {})
        }
    }

    @Test
    fun `snapshot CallControls Actions composable`() {
        snapshot {
            Column {
                Row {
                    ToggleCameraAction(
                        modifier = Modifier.size(60.dp),
                        isCameraEnabled = true,
                        onCallAction = {},
                    )
                    ToggleMicrophoneAction(
                        modifier = Modifier.size(60.dp),
                        isMicrophoneEnabled = true,
                        onCallAction = {},
                    )
                    FlipCameraAction(modifier = Modifier.size(60.dp), onCallAction = {})
                    ChatDialogAction(modifier = Modifier.size(60.dp), onCallAction = {})
                }
                Row {
                    LeaveCallAction(modifier = Modifier.size(60.dp), onCallAction = {})
                    AcceptCallAction(modifier = Modifier.size(60.dp), onCallAction = {})
                    CancelCallAction(modifier = Modifier.size(60.dp), onCallAction = {})
                }
            }
        }
    }
}
