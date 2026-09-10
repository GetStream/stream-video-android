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

package io.getstream.video.android.compose.ui.components.call.lobby

import io.getstream.video.android.compose.permission.VideoPermissionsState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import org.junit.Assert.assertEquals
import org.junit.Test

internal class LobbyControlsCallActionHandlerTest {

    private class FakePermissions(
        override val isCameraPermissionDenied: Boolean,
        override val isMicrophonePermissionDenied: Boolean,
    ) : VideoPermissionsState {
        var requests = 0
        override val allPermissionsGranted: Boolean = false
        override val shouldShowRationale: Boolean = false
        override fun launchPermissionRequest() {
            requests++
        }
    }

    private val forwarded = mutableListOf<CallAction>()

    @Test
    fun `camera toggle without the camera permission requests it and is forwarded`() {
        val permissions =
            FakePermissions(isCameraPermissionDenied = true, isMicrophonePermissionDenied = false)
        val handler = lobbyControlsCallActionHandler(permissions) { forwarded += it }

        handler(ToggleCamera(isEnabled = true))

        assertEquals(1, permissions.requests)
        assertEquals(listOf<CallAction>(ToggleCamera(isEnabled = true)), forwarded)
    }

    @Test
    fun `microphone toggle without the microphone permission requests it and is forwarded`() {
        val permissions =
            FakePermissions(isCameraPermissionDenied = false, isMicrophonePermissionDenied = true)
        val handler = lobbyControlsCallActionHandler(permissions) { forwarded += it }

        handler(ToggleMicrophone(isEnabled = true))

        assertEquals(1, permissions.requests)
        assertEquals(listOf<CallAction>(ToggleMicrophone(isEnabled = true)), forwarded)
    }

    @Test
    fun `toggles with their permission granted are only forwarded`() {
        val permissions =
            FakePermissions(isCameraPermissionDenied = false, isMicrophonePermissionDenied = false)
        val handler = lobbyControlsCallActionHandler(permissions) { forwarded += it }

        handler(ToggleCamera(isEnabled = false))
        handler(ToggleMicrophone(isEnabled = false))

        assertEquals(0, permissions.requests)
        assertEquals(2, forwarded.size)
    }

    @Test
    fun `turning a device off never requests its permission`() {
        val permissions =
            FakePermissions(isCameraPermissionDenied = true, isMicrophonePermissionDenied = true)
        val handler = lobbyControlsCallActionHandler(permissions) { forwarded += it }

        handler(ToggleCamera(isEnabled = false))
        handler(ToggleMicrophone(isEnabled = false))

        assertEquals(0, permissions.requests)
        assertEquals(2, forwarded.size)
    }

    @Test
    fun `other actions never request a permission`() {
        val permissions =
            FakePermissions(isCameraPermissionDenied = true, isMicrophonePermissionDenied = true)
        val handler = lobbyControlsCallActionHandler(permissions) { forwarded += it }

        handler(ToggleSpeakerphone(isEnabled = true))

        assertEquals(0, permissions.requests)
        assertEquals(listOf<CallAction>(ToggleSpeakerphone(isEnabled = true)), forwarded)
    }
}
