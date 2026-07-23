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

package io.getstream.video.android.core.call.components

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples

/**
 * Unit tests for [CallMediaManager], which owns the media pipeline: the peer-connection
 * factory lifecycle, screen sharing, settings-driven device init and audio processing.
 *
 * The [MediaManagerImpl] is injected through [Call.testInstanceProvider] so no real
 * WebRTC / native resources are created.
 */
class CallMediaManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var clientImpl: StreamVideoClient
    private lateinit var state: CallState
    private lateinit var sessionFlow: MutableStateFlow<RtcSession?>
    private lateinit var mediaManager: MediaManagerImpl
    private lateinit var call: Call

    @Before
    fun setup() {
        clientImpl = mockk(relaxed = true)
        state = mockk(relaxed = true)
        sessionFlow = MutableStateFlow(null)
        mediaManager = mockk(relaxed = true)
        call = mockk(relaxed = true)

        every { call.type } returns "default"
        every { call.id } returns "call-id"
        every { call.clientImpl } returns clientImpl
        every { call.state } returns state
        every { call.scope } returns testScope
        every { call.session } returns sessionFlow

        Call.testInstanceProvider.mediaManagerCreator = { mediaManager }
    }

    @After
    fun tearDown() {
        Call.testInstanceProvider.mediaManagerCreator = null
    }

    private fun manager() = CallMediaManager(call)

    @Test
    fun `startScreenSharing enables screen share when the capability is granted`() {
        every { state.ownCapabilities } returns MutableStateFlow(listOf(OwnCapability.Screenshare))

        manager().startScreenSharing(mockk<Intent>(relaxed = true), includeAudio = true)

        verify { mediaManager.screenShare.enable(any(), any(), any()) }
    }

    @Test
    fun `startScreenSharing is ignored without the screenshare capability`() {
        every { state.ownCapabilities } returns MutableStateFlow(emptyList())

        manager().startScreenSharing(mockk<Intent>(relaxed = true))

        verify(exactly = 0) { mediaManager.screenShare.enable(any(), any(), any()) }
    }

    @Test
    fun `stopScreenSharing disables screen share`() {
        manager().stopScreenSharing()
        verify { mediaManager.screenShare.disable(fromUser = true) }
    }

    @Test
    fun `cleanup delegates to the media manager`() {
        manager().cleanup()
        verify { mediaManager.cleanup() }
    }

    @Test
    fun `recreatePeerConnectionFactory is a no-op when no factory exists`() {
        // No factory created yet -> must not throw and nothing to dispose.
        manager().recreatePeerConnectionFactory()
    }

    @Test
    fun `recreateFactoryAndAudioTracks disposes existing tracks and sources`() {
        manager().recreateFactoryAndAudioTracks()
        verify { mediaManager.disposeTracksAndSources() }
    }

    @Test
    fun `ensureFactoryMatchesAudioProfile returns early when no factory exists`() {
        // Nothing to compare against -> must not throw.
        manager().ensureFactoryMatchesAudioProfile()
    }

    @Test
    fun `updateMediaManagerFromSettings does not crash for already-selected devices`() {
        val settings = mockk<CallSettingsResponse>(relaxed = true)
        manager().updateMediaManagerFromSettings(settings)
    }

    @Test
    fun `processAudioSample forwards samples to the sound processor`() {
        val samples = mockk<AudioSamples>(relaxed = true)
        every { samples.data } returns ByteArray(8)
        // Should not throw.
        manager().processAudioSample(samples)
    }

    @Test
    fun `audio-processing toggles delegate to the injected factory`() {
        val factory = mockk<StreamPeerConnectionFactory>(relaxed = true)
        every { factory.isAudioProcessingEnabled() } returns true
        every { factory.toggleAudioProcessing() } returns false

        val manager = manager()
        manager.peerConnectionFactory = factory

        assertThat(manager.peerConnectionFactory).isSameInstanceAs(factory)
        assertThat(manager.isAudioProcessingEnabled()).isTrue()
        manager.setAudioProcessingEnabled(true)
        assertThat(manager.toggleAudioProcessing()).isFalse()

        verify { factory.isAudioProcessingEnabled() }
        verify { factory.setAudioProcessingEnabled(true) }
        verify { factory.toggleAudioProcessing() }
    }

    @Test
    fun `localMicrophoneAudioLevel is exposed`() {
        assertThat(manager().localMicrophoneAudioLevel.value).isEqualTo(0f)
    }

    @Test
    fun `updateMediaManagerFromSettings initialises not-yet-selected devices`() = runTest(
        testDispatcher,
    ) {
        every { mediaManager.speaker.status } returns MutableStateFlow(DeviceStatus.NotSelected)
        every { mediaManager.camera.status } returns MutableStateFlow(DeviceStatus.NotSelected)
        every { mediaManager.microphone.status } returns MutableStateFlow(DeviceStatus.NotSelected)
        every { mediaManager.microphone.devices } returns MutableStateFlow(emptyList())

        manager().updateMediaManagerFromSettings(mockk(relaxed = true))
        advanceUntilIdle()

        verify { mediaManager.speaker.setEnabled(any()) }
        verify { mediaManager.camera.setEnabled(any()) }
        verify { mediaManager.microphone.setEnabled(any()) }
    }

    @Test
    fun `monitorHeadset selects a bluetooth headset when available`() = runTest(testDispatcher) {
        val bluetooth = mockk<StreamAudioDevice.BluetoothHeadset>(relaxed = true)
        every { mediaManager.microphone.devices } returns MutableStateFlow(listOf(bluetooth))

        manager().updateMediaManagerFromSettings(mockk(relaxed = true))
        advanceUntilIdle()

        verify { mediaManager.microphone.select(bluetooth) }
    }

    @Test
    fun `monitorHeadset selects a wired headset when no bluetooth is present`() = runTest(
        testDispatcher,
    ) {
        val wired = mockk<StreamAudioDevice.WiredHeadset>(relaxed = true)
        every { mediaManager.microphone.devices } returns MutableStateFlow(listOf(wired))

        manager().updateMediaManagerFromSettings(mockk(relaxed = true))
        advanceUntilIdle()

        verify { mediaManager.microphone.select(wired) }
    }
}
