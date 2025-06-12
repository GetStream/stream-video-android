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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.RtpParameters.Encoding
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.RtpTransceiver.RtpTransceiverInit
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
                tracer = mockk(relaxed = true),
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

    @Test
    fun `buildVideoTransceiverInit camera has three encodings q,h,f`() = runTest {
        // We'll create a StreamPeerConnection with a known maxBitRate
        val peerConnection = object : StreamPeerConnection(
            coroutineScope = testScope, // or any TestScope
            type = io.getstream.video.android.core.model.StreamPeerType.PUBLISHER,
            mediaConstraints = org.webrtc.MediaConstraints(),
            onStreamAdded = null,
            onNegotiationNeeded = null,
            onIceCandidate = null,
            maxBitRate = 2_000_000,
            tracer = mockk(relaxed = true),
        ) {}

        val streamIds = listOf("camera-stream-id")
        val init = peerConnection.buildVideoTransceiverInit(
            streamIds = streamIds,
            isScreenShare = false, // camera scenario
        )

        // Check direction
        assertEquals(
            "Direction should be SEND_ONLY",
            RtpTransceiverDirection.SEND_ONLY,
            init.actualDirection(),
        )
        // Check stream IDs
        // Expect 3 encodings: q, h, f
        val encodings = init.actualEncodings()
        assertEquals("Should have three encodings for camera", 3, encodings?.size)

        val (q, h, f) = encodings!!
        // Check q
        assertEquals("Wrong rid for q", "q", q.rid)
        assertTrue("q should be active", q.active)
        // 2_000_000 / 4 => 500,000
        assertEquals("Wrong quarter maxBitrateBps", 500_000, q.maxBitrateBps)
        assertEquals("Wrong q maxFramerate", 30, q.maxFramerate)

        // Check h
        assertEquals("Wrong rid for h", "h", h.rid)
        assertTrue("h should be active", h.active)
        // 2_000_000 / 2 => 1,000,000
        assertEquals("Wrong half maxBitrateBps", 1_000_000, h.maxBitrateBps)
        assertEquals("Wrong h maxFramerate", 30, h.maxFramerate)

        // Check f
        assertEquals("Wrong rid for f", "f", f.rid)
        assertTrue("f should be active", f.active)
        // full => 2,000,000
        assertEquals("Wrong full maxBitrateBps", 2_000_000, f.maxBitrateBps)
        assertEquals("Wrong f maxFramerate", 30, f.maxFramerate)
    }

    @Test
    fun `buildVideoTransceiverInit screenshare has one encoding q`() = runTest {
        val peerConnection = object : StreamPeerConnection(
            coroutineScope = testScope,
            type = io.getstream.video.android.core.model.StreamPeerType.SUBSCRIBER,
            mediaConstraints = org.webrtc.MediaConstraints(),
            onStreamAdded = null,
            onNegotiationNeeded = null,
            onIceCandidate = null,
            maxBitRate = 2_000_000,
            tracer = mockk(relaxed = true),
        ) {}

        val streamIds = listOf("screen-stream-id")
        val init = peerConnection.buildVideoTransceiverInit(
            streamIds = streamIds,
            isScreenShare = true,
        )

        assertEquals(
            "Direction should be SEND_ONLY",
            init.actualDirection(),
            RtpTransceiverDirection.SEND_ONLY,
        )

        // Expect 1 encoding: q
        val encodings = init.actualEncodings()
        assertEquals("Should have one encoding for screenshare", 1, encodings?.size)

        val q = encodings!![0]
        assertEquals("Wrong rid for screenshare", "q", q.rid)
        assertTrue("q should be active", q.active)
        // Hard-coded to 1,000,000 for screenshare
        assertEquals("Wrong screenshare maxBitrateBps", 1_000_000, q.maxBitrateBps)
        // No explicit frame rate is set in the code for screenshare
        assertEquals("Screenshare maxFramerate default", null, q.maxFramerate)
    }

    // Utils

    fun RtpTransceiverInit.actualDirection(): RtpTransceiverDirection? {
        val directionField = RtpTransceiverInit::class.java.getDeclaredField("direction")
        directionField.isAccessible = true
        val actualDirection = directionField.get(this) as? RtpTransceiverDirection
        return actualDirection
    }

    fun RtpTransceiverInit.actualEncodings(): List<Encoding>? {
        val encodingsField = RtpTransceiverInit::class.java.getDeclaredField("sendEncodings")
        encodingsField.isAccessible = true
        val actualEncodings = encodingsField.get(this) as? List<Encoding>
        return actualEncodings
    }
}
