/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.connection

import io.getstream.result.Result
import io.getstream.video.android.core.model.StreamPeerType
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.test.Test

class StreamPeerConnectionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var peerConnection: PeerConnection
    private lateinit var streamPeerConnection: StreamPeerConnection

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock the real PeerConnection
        peerConnection = mockk(relaxed = true)

        // We'll create a real or spy StreamPeerConnection
        streamPeerConnection = spyk(
            object : StreamPeerConnection(
                coroutineScope = testScope,
                type = StreamPeerType.PUBLISHER,
                mediaConstraints = MediaConstraints(),
                onStreamAdded = null,
                onNegotiationNeeded = null,
                onIceCandidate = null,
                maxBitRate = 2_000_000,
            ) {},
        ) {
            every { initialize(any()) } just Runs
            every { connection } returns peerConnection
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `isHealthy returns true for NEW, CONNECTED, or CONNECTING`() {
        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.NEW
        assertTrue("Should be healthy for NEW", streamPeerConnection.isHealthy())

        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.CONNECTED
        assertTrue("Should be healthy for CONNECTED", streamPeerConnection.isHealthy())

        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.CONNECTING
        assertTrue("Should be healthy for CONNECTING", streamPeerConnection.isHealthy())
    }

    @Test
    fun `isHealthy returns false otherwise`() {
        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.DISCONNECTED
        assertFalse("DISCONNECTED not healthy", streamPeerConnection.isHealthy())

        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.FAILED
        assertFalse("FAILED not healthy", streamPeerConnection.isHealthy())

        streamPeerConnection.state.value = null
        assertFalse("null state not healthy", streamPeerConnection.isHealthy())
    }

    @Test
    fun `isFailedOrClosed returns true for CLOSED or FAILED`() {
        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.CLOSED
        assertTrue("CLOSED is failedOrClosed", streamPeerConnection.isFailedOrClosed())

        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.FAILED
        assertTrue("FAILED is failedOrClosed", streamPeerConnection.isFailedOrClosed())
    }

    @Test
    fun `isFailedOrClosed returns false otherwise`() {
        streamPeerConnection.state.value = PeerConnection.PeerConnectionState.CONNECTED
        assertFalse(
            "CONNECTED is not failedOrClosed",
            streamPeerConnection.isFailedOrClosed(),
        )
    }

    @Test
    fun `createOffer calls connectioncreateOffer`() = testScope.runTest {
        // Mock webrtc createOffer
        coEvery {
            peerConnection.createOffer(any(), any())
        } answers {
            val cb = firstArg<SdpObserver>()
            cb.onCreateSuccess(SessionDescription(SessionDescription.Type.OFFER, "mock-offer"))
        }

        streamPeerConnection.initialize(peerConnection)
        val result = streamPeerConnection.createOffer()
        assertTrue("Should succeed", result is Result.Success)
        // assertEquals("mock-offer", (result as Result.Success).data.description)
    }

    @Test
    fun `createAnswer calls connectioncreateAnswer`() = testScope.runTest {
        coEvery {
            peerConnection.createAnswer(any(), any())
        } answers {
            val cb = firstArg<SdpObserver>()
            cb.onCreateSuccess(SessionDescription(SessionDescription.Type.ANSWER, "mock-answer"))
        }

        streamPeerConnection.initialize(peerConnection)
        val result = streamPeerConnection.createAnswer()
        assertTrue("Should succeed", result is Result.Success)
    }
}
