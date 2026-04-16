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

package io.getstream.video.android.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.ParcelUuid
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import com.twilio.audioswitch.AudioDevice
import io.getstream.android.video.generated.models.AudioSettingsResponse
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.audio.UsbAudioInputDevice
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.TelecomCall
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.TelecomCallAction
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.webrtc.AudioTrack
import stream.video.sfu.models.AudioBitrateProfile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class MicrophoneManagerTest {

    private val audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION
    private val audioUsageProvider = { AudioAttributes.USAGE_VOICE_COMMUNICATION }

    @Test
    fun `Don't crash when accessing audioHandler prior to setup`() {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val actual =
            MicrophoneManager(
                mediaManager,
                audioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION,
                audioUsageProvider = { AudioAttributes.USAGE_VOICE_COMMUNICATION },
            )
        val context = mockk<Context>(relaxed = true)
        val microphoneManager = spyk(actual)
        every { mediaManager.context } returns context
        every { context.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)

        // When
        microphoneManager.cleanup()

        // Then
        verify(exactly = 0) {
            // Setup was not called, but still there is no exception
            microphoneManager.setup()
        }
    }

    @Test
    fun `resume calls setup again after cleanup`() {
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val microphoneManager =
            spyk(MicrophoneManager(mediaManager, audioUsage, audioUsageProvider))

        every { mediaManager.context } returns context
        every { context.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)
        every { microphoneManager.setup(any(), any()) } answers {
            secondArg<(() -> Unit)?>()?.invoke()
        }

        microphoneManager.setup()
        microphoneManager.cleanup()
        microphoneManager.resume()

        verifyOrder {
            microphoneManager.setup()
            microphoneManager.cleanup()
            microphoneManager.resume()
            microphoneManager.setup(any(), any())
        }
    }

    @Test
    fun `Resume will call enable only if prior status was DeviceStatus#enabled`() {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)
        val spyMicrophoneManager = spyk(microphoneManager)
        val mockContext = mockk<Context>(relaxed = true)
        val mockCallState = mockk<CallState>(relaxed = true)

        every { mediaManager.context } returns mockContext
        every { mockContext.getSystemService(any()) } returns mockk<AudioManager>(relaxed = true)
        every { mediaManager.call.state } returns mockCallState
        every { mockCallState.ownCapabilities.value } returns listOf(OwnCapability.SendAudio)

        val slot = slot<() -> Unit>()
        every { spyMicrophoneManager.setup(any(), capture(slot)) } answers { slot.captured.invoke() }

        // When
        spyMicrophoneManager.priorStatus = DeviceStatus.Enabled
        spyMicrophoneManager.resume() // Calls setup internally

        // Then
        verify(exactly = 1) {
            // Setup was called twice
            spyMicrophoneManager.enable()
        }
    }

    @Test
    fun `enable should update status and enable audio track`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        val microphoneManager = microphoneManagerWithImmediateSetup(audioTrack = audioTrack)

        microphoneManager.enable(fromUser = true)

        assertEquals(DeviceStatus.Enabled, microphoneManager.status.value)
        verify(exactly = 1) { audioTrack.trySetEnabled(true) }
    }

    @Test
    fun `disable should update status and disable audio track`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        val microphoneManager = microphoneManagerWithImmediateSetup(audioTrack = audioTrack)

        microphoneManager.disable(fromUser = true)

        assertEquals(DeviceStatus.Disabled, microphoneManager.status.value)
        verify(exactly = 1) { audioTrack.trySetEnabled(false) }
    }

    @Test
    fun `enable should NOT override status when fromUser is false`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        val microphoneManager = microphoneManagerWithImmediateSetup(audioTrack = audioTrack)
        microphoneManager.disable(fromUser = true)
        clearMocks(audioTrack)

        microphoneManager.enable(fromUser = false)

        assertEquals(DeviceStatus.Disabled, microphoneManager.status.value)
        verify(exactly = 1) { audioTrack.trySetEnabled(true) }
    }

    @Test
    fun `pause should store prior status and disable`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        val microphoneManager = microphoneManagerWithImmediateSetup(audioTrack = audioTrack)
        microphoneManager.enable(fromUser = true)
        clearMocks(audioTrack)

        microphoneManager.pause(fromUser = true)

        assertEquals(DeviceStatus.Enabled, microphoneManager.priorStatus)
        assertEquals(DeviceStatus.Disabled, microphoneManager.status.value)
        verify(exactly = 1) { audioTrack.trySetEnabled(false) }
    }

    @Test
    fun `resume should restore enabled state if previously enabled`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        val microphoneManager = microphoneManagerWithImmediateSetup(audioTrack = audioTrack)
        microphoneManager.enable(fromUser = true)
        microphoneManager.pause(fromUser = true)
        clearMocks(audioTrack)

        microphoneManager.resume(fromUser = true)

        assertEquals(DeviceStatus.Enabled, microphoneManager.status.value)
        verify(exactly = 1) { audioTrack.trySetEnabled(true) }
    }

    @Test
    fun `resume should NOT enable if prior status was not enabled`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        val microphoneManager = microphoneManagerWithImmediateSetup(audioTrack = audioTrack)
        microphoneManager.disable(fromUser = true)
        microphoneManager.pause(fromUser = true)
        clearMocks(audioTrack)

        microphoneManager.resume(fromUser = true)

        assertEquals(DeviceStatus.Disabled, microphoneManager.status.value)
        verify(exactly = 0) { audioTrack.trySetEnabled(true) }
    }

    @Test
    fun `setEnabled true should call enable`() {
        val microphoneManager = microphoneManagerWithImmediateSetup()

        microphoneManager.setEnabled(true)

        verify(exactly = 1) { microphoneManager.enable(true) }
    }

    @Test
    fun `setEnabled false should call disable`() {
        val microphoneManager = microphoneManagerWithImmediateSetup()

        microphoneManager.setEnabled(false)

        verify(exactly = 1) { microphoneManager.disable(true) }
    }

    @Test
    fun `select should update selected device`() {
        val mediaManager = mockMediaManager()
        val speakerManager = mockSpeakerManager(isEnabled = false)
        every { mediaManager.speaker } returns speakerManager
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)
        val selectedDevice = StreamAudioDevice.Earpiece(audio = mockk<AudioDevice>())

        microphoneManager.select(selectedDevice)

        assertEquals(selectedDevice, microphoneManager.selectedDevice.value)
    }

    @Test
    fun `select speaker should enable speaker status`() {
        val mediaManager = mockMediaManager()
        val speakerStatus = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
        val speakerManager = mockSpeakerManager(isEnabled = false, status = speakerStatus)
        every { mediaManager.speaker } returns speakerManager
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        microphoneManager.select(StreamAudioDevice.Speakerphone(audio = mockk<AudioDevice>()))

        assertEquals(DeviceStatus.Enabled, speakerStatus.value)
    }

    @Test
    fun `select non headset should update fallback device`() {
        val mediaManager = mockMediaManager()
        val speakerManager = mockSpeakerManager(isEnabled = false)
        every { mediaManager.speaker } returns speakerManager
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)
        val earpiece = StreamAudioDevice.Earpiece(audio = mockk<AudioDevice>())

        microphoneManager.select(earpiece)

        assertEquals(earpiece, microphoneManager.nonHeadsetFallbackDevice)
    }

    @Test
    fun `select should switch telecom endpoint when matching endpoint exists`() {
        val endpointId = ParcelUuid.fromString("11111111-1111-1111-1111-111111111111")
        val speakerEndpoint = mockTelecomEndpoint(
            type = CallEndpointCompat.TYPE_SPEAKER,
            identifier = endpointId,
            name = "Speaker",
        )
        val telecomCall =
            spyk(
                registeredTelecomCall(
                    currentEndpoint = null,
                    availableEndpoints = listOf(speakerEndpoint),
                ),
            )
        val mediaManager = mockMediaManagerWithTelecom(telecomCall)
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        microphoneManager.select(StreamAudioDevice.Speakerphone(audio = mockk<AudioDevice>()))

        verify(exactly = 1) {
            telecomCall.processAction(TelecomCallAction.SwitchAudioEndpoint(endpointId))
        }
    }

    @Test
    fun `select should not switch telecom endpoint when endpoint already selected`() {
        val endpointId = ParcelUuid.fromString("22222222-2222-2222-2222-222222222222")
        val earpieceEndpoint = mockTelecomEndpoint(
            type = CallEndpointCompat.TYPE_EARPIECE,
            identifier = endpointId,
            name = "Earpiece",
        )
        val telecomCall = registeredTelecomCall(
            currentEndpoint = earpieceEndpoint,
            availableEndpoints = listOf(earpieceEndpoint),
        )
        val mediaManager = mockMediaManagerWithTelecom(telecomCall)
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        microphoneManager.select(StreamAudioDevice.Earpiece(audio = mockk<AudioDevice>()))

        assertNull(telecomCall.actionSource.tryReceive().getOrNull())
    }

    @Test
    fun `select should not switch telecom endpoint when no matching endpoint exists`() {
        val bluetoothEndpoint = mockTelecomEndpoint(
            type = CallEndpointCompat.TYPE_BLUETOOTH,
            identifier = ParcelUuid.fromString("33333333-3333-3333-3333-333333333333"),
            name = "Bluetooth",
        )
        val telecomCall = registeredTelecomCall(
            currentEndpoint = null,
            availableEndpoints = listOf(bluetoothEndpoint),
        )
        val mediaManager = mockMediaManagerWithTelecom(telecomCall)
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        microphoneManager.select(StreamAudioDevice.WiredHeadset(audio = mockk<AudioDevice>()))

        assertNull(telecomCall.actionSource.tryReceive().getOrNull())
    }

    @Test
    fun `selectUsbDevice success should update state`() {
        val mediaManager = mockMediaManager()
        val call = mockk<Call>(relaxed = true)
        val peerConnectionFactory = mockk<StreamPeerConnectionFactory>()
        val deviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        val usbDevice = UsbAudioInputDevice(deviceInfo)
        every { mediaManager.call } returns call
        every { call.peerConnectionFactory } returns peerConnectionFactory
        every { peerConnectionFactory.setPreferredAudioInputDevice(deviceInfo) } returns true

        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        val result = microphoneManager.selectUsbDevice(usbDevice)

        assertTrue(result)
        assertEquals(usbDevice, microphoneManager.selectedUsbDevice.value)
    }

    @Test
    fun `selectUsbDevice failure should NOT update state`() {
        val mediaManager = mockMediaManager()
        val call = mockk<Call>(relaxed = true)
        val peerConnectionFactory = mockk<StreamPeerConnectionFactory>()
        val deviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        val usbDevice = UsbAudioInputDevice(deviceInfo)
        every { mediaManager.call } returns call
        every { call.peerConnectionFactory } returns peerConnectionFactory
        every { peerConnectionFactory.setPreferredAudioInputDevice(deviceInfo) } returns false

        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        val result = microphoneManager.selectUsbDevice(usbDevice)

        assertFalse(result)
        assertNull(microphoneManager.selectedUsbDevice.value)
    }

    @Test
    fun `setAudioBitrateProfile should fail if call already joined`() = runTest {
        val mediaManager = mockMediaManager(
            connection = MutableStateFlow(
                RealtimeConnection.Joined(mockk<RtcSession>(relaxed = true)),
            ),
            settings = MutableStateFlow(mockCallSettings(hifiAudioEnabled = true)),
        )
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        val result = microphoneManager.setAudioBitrateProfile(
            AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_HIGH_QUALITY,
        )

        assertTrue(result.isFailure)
        assertEquals(
            AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED,
            microphoneManager.audioBitrateProfile.value,
        )
    }

    @Test
    fun `setAudioBitrateProfile should fail if hifi disabled`() = runTest {
        val mediaManager = mockMediaManager(
            connection = MutableStateFlow(RealtimeConnection.PreJoin),
            settings = MutableStateFlow(mockCallSettings(hifiAudioEnabled = false)),
        )
        val microphoneManager = MicrophoneManager(mediaManager, audioUsage, audioUsageProvider)

        val result = microphoneManager.setAudioBitrateProfile(
            AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY,
        )

        assertTrue(result.isFailure)
        assertEquals(
            AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED,
            microphoneManager.audioBitrateProfile.value,
        )
    }

    private fun microphoneManagerWithImmediateSetup(
        mediaManager: MediaManagerImpl = mockMediaManager(),
        audioTrack: AudioTrack = mockk(relaxed = true),
    ): MicrophoneManager {
        every { mediaManager.audioTrack } returns audioTrack
        every { mediaManager.speaker } returns mockSpeakerManager(isEnabled = false)

        val microphoneManager =
            spyk(MicrophoneManager(mediaManager, audioUsage, audioUsageProvider))
        every { microphoneManager.setup(any(), any()) } answers {
            secondArg<(() -> Unit)?>()?.invoke()
        }
        return microphoneManager
    }

    private fun mockMediaManager(
        connection: MutableStateFlow<RealtimeConnection> =
            MutableStateFlow(RealtimeConnection.PreJoin),
        settings: MutableStateFlow<CallSettingsResponse?> =
            MutableStateFlow(mockCallSettings(true)),
        call: Call = mockk(relaxed = true),
        callState: CallState = mockk(relaxed = true),
    ): MediaManagerImpl {
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        every { mediaManager.call } returns call
        every { call.state } returns callState
        every { callState.connection } returns connection
        every { callState.settings } returns settings
        every { callState.jetpackTelecomRepository } returns null
        return mediaManager
    }

    private fun mockMediaManagerWithTelecom(telecomCall: TelecomCall.Registered): MediaManagerImpl {
        val callState = mockk<CallState>(relaxed = true)
        val telecomRepository = mockk<JetpackTelecomRepository>(relaxed = true)
        every { telecomRepository.currentCall } returns MutableStateFlow<TelecomCall>(telecomCall)
        return mockMediaManager(callState = callState).also {
            every { callState.jetpackTelecomRepository } returns telecomRepository
            every { it.speaker } returns mockSpeakerManager(isEnabled = false)
        }
    }

    private fun mockSpeakerManager(
        isEnabled: Boolean,
        status: MutableStateFlow<DeviceStatus> = MutableStateFlow(DeviceStatus.NotSelected),
    ): SpeakerManager {
        val speakerManager = mockk<SpeakerManager>(relaxed = true)
        every { speakerManager.isEnabled } returns MutableStateFlow(isEnabled)
        every { speakerManager._status } returns status
        return speakerManager
    }

    private fun mockCallSettings(hifiAudioEnabled: Boolean): CallSettingsResponse {
        val audioSettings = mockk<AudioSettingsResponse>(relaxed = true)
        every { audioSettings.hifiAudioEnabled } returns hifiAudioEnabled

        return mockk(relaxed = true) {
            every { audio } returns audioSettings
        }
    }

    private fun registeredTelecomCall(
        currentEndpoint: CallEndpointCompat?,
        availableEndpoints: List<CallEndpointCompat>,
    ): TelecomCall.Registered {
        return TelecomCall.Registered(
            id = ParcelUuid.fromString("00000000-0000-0000-0000-000000000001"),
            callAttributes = CallAttributesCompat(
                displayName = "Test",
                address = Uri.parse("tel:123456789"),
                direction = CallAttributesCompat.DIRECTION_OUTGOING,
                callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                callCapabilities = 0,
            ),
            isActive = true,
            isOnHold = false,
            isMuted = false,
            errorCode = null,
            currentCallEndpoint = currentEndpoint,
            availableCallEndpoints = availableEndpoints,
            actionSource = Channel(Channel.UNLIMITED),
        )
    }

    private fun mockTelecomEndpoint(
        type: Int,
        identifier: ParcelUuid,
        name: String,
    ): CallEndpointCompat {
        return CallEndpointCompat(name, type, identifier)
    }
}
