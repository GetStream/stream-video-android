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

import android.content.Context
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.api.SignalServerService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.webrtc.EglBase
import org.webrtc.ManagedAudioProcessingFactory
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.Observer
import org.webrtc.PeerConnectionFactory
import stream.video.sfu.models.PublishOption

class StreamPeerConnectionFactoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @RelaxedMockK
    lateinit var mockContext: Context

    @RelaxedMockK
    lateinit var mockParticipantState: ParticipantState

    @RelaxedMockK
    lateinit var mockMediaManager: MediaManagerImpl

    @RelaxedMockK
    lateinit var mockSignalServerService: SignalServerService

    private lateinit var factory: StreamPeerConnectionFactory
    private lateinit var mockPeerConnection: PeerConnection
    private lateinit var mockInternalFactory: PeerConnectionFactory

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Create a spied factory with a provider that returns a mock EglBase
        // Using a provider means EglBase.create() won't be called during construction
        // The mock will only be created when the provider is invoked (lazy evaluation)
        val mockEglBaseProvider: () -> EglBase = {
            // This will be called lazily when eglBase is accessed
            // Since we're using relaxed mocks, MockK should be able to create it
            // even if Android classes aren't available, as long as we don't call methods
            // that require those classes
            mockk(relaxed = true)
        }

        factory = spyk(StreamPeerConnectionFactory(mockContext, sharedEglBaseProvider = mockEglBaseProvider))
        // Mock the internal WebRTC PeerConnectionFactory
        mockInternalFactory = mockk(relaxed = true)

        // Mock a PeerConnection to return from makePeerConnectionInternal
        mockPeerConnection = mockk(relaxed = true)
        // We'll override the internal creation method so it returns our mock
        every {
            factory.makePeerConnectionInternal(any<PeerConnection.RTCConfiguration>(), any())
        } returns mockPeerConnection
    }

    @After
    fun tearDown() {
        // No special teardown required in this minimal example
    }

    @Test
    fun `makePublisher returns a properly initialized Publisher`() {
        val fakePublishOptions = listOf(
            PublishOption(
                id = 1,
                track_type = stream.video.sfu.models.TrackType.TRACK_TYPE_VIDEO,
                bitrate = 500_000,
                fps = 30,
                max_spatial_layers = 1,
                max_temporal_layers = 1,
                codec = null,
                video_dimension = null,
            ),
        )

        val fakeConfig = PeerConnection.RTCConfiguration(emptyList())
        val fakeConstraints = MediaConstraints()

        // We're testing makePublisher, so we pass in all required arguments
        val publisher = factory.makePublisher(
            mediaManager = mockMediaManager,
            publishOptions = fakePublishOptions,
            coroutineScope = testScope,
            configuration = fakeConfig,
            mediaConstraints = fakeConstraints,
            onStreamAdded = null,
            onNegotiationNeeded = { _, _ -> },
            onIceCandidate = null,
            maxPublishingBitrate = 1_200_000,
            sfuClient = mockSignalServerService,
            sessionId = "fake-session-id",
            tracer = mockk(relaxed = true),
            rejoin = {},
        )

        assertNotNull("Publisher should not be null", publisher)
        assertTrue("Publisher should be instance of Publisher", publisher is Publisher)
        // The peerConnection inside publisher should be the one we mocked
        assertEquals(
            "Publisher.connection should match the mocked PeerConnection",
            mockPeerConnection,
            publisher.connection,
        )

        // Verify that makePeerConnectionInternal was called with the config and 'observer' = publisher
        verify {
            factory.makePeerConnectionInternal(fakeConfig, publisher as PeerConnection.Observer)
        }
    }

    @Test
    fun `makePeerConnection creates a StreamPeerConnection and initializes it`() {
        val mockObserverPc = mockk<PeerConnection>(relaxed = true)
        every {
            mockInternalFactory.createPeerConnection(any<PeerConnection.RTCConfiguration>(), any<Observer>())
        } returns mockObserverPc

        val config = PeerConnection.RTCConfiguration(emptyList())
        val result = factory.makePeerConnection(
            configuration = config,
            type = io.getstream.video.android.core.model.StreamPeerType.SUBSCRIBER,
            mediaConstraints = MediaConstraints(),
            onStreamAdded = null,
            onNegotiationNeeded = null,
            onIceCandidateRequest = null,
            debugText = "test",
            maxPublishingBitrate = 999_999,
        )

        assertNotNull("Result should not be null", result)
        assertTrue("Should be a StreamPeerConnection", result is StreamPeerConnection)
    }

    @Test
    fun `isAudioProcessingEnabled returns false if audioProcessing null`() {
        // By default we haven't set audioProcessing. So it should be null => false
        val result = factory.isAudioProcessingEnabled()
        assertFalse("Should be false if audioProcessing is null", result)
    }

    @Test
    fun `isAudioProcessingEnabled returns true if audioProcessing is set and enabled`() {
        // We'll inject a ManagedAudioProcessingFactory mock
        val mockAudioProc = mockk<ManagedAudioProcessingFactory>(relaxed = true)
        every { mockAudioProc.isEnabled } returns true

        // Reflection hack to set audioProcessing
        StreamPeerConnectionFactory::class.java
            .getDeclaredField("audioProcessing")
            .apply {
                isAccessible = true
                set(factory, mockAudioProc)
            }

        val result = factory.isAudioProcessingEnabled()
        assertTrue("Should be true if isEnabled is true", result)
    }

    // 8) Test setAudioProcessingEnabled
    @Test
    fun `setAudioProcessingEnabled does nothing if audioProcessing is null`() {
        factory.setAudioProcessingEnabled(true)
        // no crash => pass
    }

    @Test
    fun `setAudioProcessingEnabled updates audioProcessing isEnabled`() {
        val mockAudioProc = mockk<ManagedAudioProcessingFactory>(relaxed = true)
        every { mockAudioProc.isEnabled } returns false

        StreamPeerConnectionFactory::class.java
            .getDeclaredField("audioProcessing")
            .apply {
                isAccessible = true
                set(factory, mockAudioProc)
            }

        factory.setAudioProcessingEnabled(true)
        verify { mockAudioProc.isEnabled = true }

        factory.setAudioProcessingEnabled(false)
        verify { mockAudioProc.isEnabled = false }
    }

    // 9) Test toggleAudioProcessing
    @Test
    fun `toggleAudioProcessing returns false if audioProcessing null`() {
        val result = factory.toggleAudioProcessing()
        assertFalse("Returns false if audioProcessing is null", result)
    }

    @Test
    fun `toggleAudioProcessing inverts the isEnabled field`() {
        val mockAudioProc = mockk<ManagedAudioProcessingFactory>(relaxed = true)
        every { mockAudioProc.isEnabled } returnsMany listOf(false, true, false)

        StreamPeerConnectionFactory::class.java
            .getDeclaredField("audioProcessing")
            .apply {
                isAccessible = true
                set(factory, mockAudioProc)
            }

        val first = factory.toggleAudioProcessing()
        assertTrue("Should now be enabled", first)
        verify { mockAudioProc.isEnabled = true }
    }
}
