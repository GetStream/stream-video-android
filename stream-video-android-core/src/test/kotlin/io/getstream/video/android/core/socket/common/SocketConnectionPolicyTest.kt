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

package io.getstream.video.android.core.socket.common

import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.OwnUserResponse
import io.getstream.result.Error
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.socket.common.ConnectionConf.UserConnectionConf
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketConnectionType
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.model.User
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

class SocketConnectionPolicyTest {

    private val user = User("test")
    private val dummyDateTime = OffsetDateTime.now(ZoneOffset.UTC)
    private val connectingState = VideoSocketState.Connecting(
        UserConnectionConf("", "", user),
        VideoSocketConnectionType.INITIAL_CONNECTION,
    )
    private val connectedState = VideoSocketState.Connected(
        ConnectedEvent(
            connectionId = "test",
            createdAt = dummyDateTime,
            me = OwnUserResponse(
                dummyDateTime,
                "test",
                "test",
                "test",
                dummyDateTime,
                emptyList(),
                emptyList(),
                emptyMap(),
            ),
            "",
        ),
    )
    private val disconnectedTemporarilyState = VideoSocketState.Disconnected.DisconnectedTemporarily(
        Error.NetworkError("Test", 400),
    )
    private val disconnectedPermanentlyState = VideoSocketState.Disconnected.DisconnectedPermanently(
        Error.NetworkError("Test", 400),
    )

    @Test
    fun `CallAwareConnectionPolicy always returns true for shouldConnect`() {
        val streamVideoFlow = MutableStateFlow<StreamVideo?>(null)
        val policy = CallAwareConnectionPolicy(streamVideoFlow)

        assertTrue(policy.shouldConnect())
    }

    @Test
    fun `CallAwareConnectionPolicy shouldDisconnect returns true when StreamVideo is null`() {
        val streamVideoFlow = MutableStateFlow<StreamVideo?>(null)
        val policy = CallAwareConnectionPolicy(streamVideoFlow)

        assertTrue(policy.shouldDisconnect())
    }

    @Test
    fun `CallAwareConnectionPolicy shouldDisconnect returns true when there is no active or ringing call`() {
        val streamVideo = mockk<StreamVideo>()
        val callState = mockk<ClientState>()
        every { streamVideo.state } returns callState
        every { callState.hasActiveOrRingingCall() } returns false
        val streamVideoFlow = MutableStateFlow<StreamVideo?>(streamVideo)
        val policy = CallAwareConnectionPolicy(streamVideoFlow)

        assertTrue(policy.shouldDisconnect())
    }

    @Test
    fun `CallAwareConnectionPolicy shouldDisconnect returns false when there is an active or ringing call`() {
        val streamVideo = mockk<StreamVideo>()
        val callState = mockk<ClientState>()
        every { streamVideo.state } returns callState
        every { callState.hasActiveOrRingingCall() } returns true
        val streamVideoFlow = MutableStateFlow<StreamVideo?>(streamVideo)
        val policy = CallAwareConnectionPolicy(streamVideoFlow)

        assertFalse(policy.shouldDisconnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns true when state is DisconnectedPermanently`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(disconnectedPermanentlyState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns true when state is DisconnectedTemporarily`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(disconnectedTemporarilyState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns true when state is NetworkDisconnected`() {
        val stateFlow =
            MutableStateFlow<VideoSocketState>(VideoSocketState.Disconnected.NetworkDisconnected)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns true when state is DisconnectedByRequest`() {
        val stateFlow =
            MutableStateFlow<VideoSocketState>(VideoSocketState.Disconnected.DisconnectedByRequest)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns true when state is Stopped`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(VideoSocketState.Disconnected.Stopped)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns false when state is Connected`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(connectedState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertFalse(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldConnect returns false when state is Connecting`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(connectingState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertFalse(policy.shouldConnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldDisconnect returns true when state is Connected`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(connectedState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldDisconnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldDisconnect returns true when state is Connecting`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(connectingState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldDisconnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldDisconnect returns true when state is DisconnectedTemporarily`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(disconnectedTemporarilyState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertTrue(policy.shouldDisconnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldDisconnect returns false when state is DisconnectedPermanently`() {
        val stateFlow = MutableStateFlow<VideoSocketState>(disconnectedPermanentlyState)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertFalse(policy.shouldDisconnect())
    }

    @Test
    fun `SocketStateConnectionPolicy shouldDisconnect returns false when state is NetworkDisconnected`() {
        val stateFlow =
            MutableStateFlow<VideoSocketState>(VideoSocketState.Disconnected.NetworkDisconnected)
        val policy = SocketStateConnectionPolicy(stateFlow)

        assertFalse(policy.shouldDisconnect())
    }
}
