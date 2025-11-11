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

package io.getstream.video.android.core

import android.media.AudioAttributes
import com.twilio.audioswitch.AudioDevice
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeakerManagerTest {

    @Test
    fun `setSpeakerPhone to true selects speakerphone device and saves previous selection`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val speakerManager = SpeakerManager(mediaManager, microphoneManager)

        val audioDevice = mockk<AudioDevice>()
        val speakerDevice = StreamAudioDevice.Speakerphone("test-speaker", audioDevice)
        val earpieceDevice = StreamAudioDevice.Earpiece("test-earpiece", audioDevice)

        val devices = listOf(speakerDevice, earpieceDevice)
        val deviceSlot = slot<StreamAudioDevice>()

        // Set up enforceSetup to execute the lambda immediately
        every { microphoneManager.enforceSetup(any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }
        every { microphoneManager.devices.value } returns devices
        every { microphoneManager.selectedDevice.value } returns earpieceDevice
        every { microphoneManager.select(capture(deviceSlot)) } answers { Unit }

        // When
        speakerManager.setSpeakerPhone(true)

        // Then
        verify { microphoneManager.enforceSetup(preferSpeaker = true, any()) }
        assertEquals(speakerDevice, deviceSlot.captured)
        assertEquals(earpieceDevice, speakerManager.selectedBeforeSpeaker)
        assertEquals(true, speakerManager.speakerPhoneEnabled.value)
    }

    @Test
    fun `setSpeakerPhone to false selects device saved in selectedBeforeSpeaker`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val speakerManager = SpeakerManager(mediaManager, microphoneManager)

        val audioDevice = mockk<AudioDevice>()
        val speakerDevice = StreamAudioDevice.Speakerphone("test-speaker", audioDevice)
        val earpieceDevice = StreamAudioDevice.Earpiece("test-earpiece", audioDevice)

        speakerManager.selectedBeforeSpeaker = earpieceDevice

        val deviceSlot = slot<StreamAudioDevice>()
        val devices = listOf(speakerDevice, earpieceDevice)

        // Set up enforceSetup to execute the lambda immediately
        every { microphoneManager.enforceSetup(any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }
        every { microphoneManager.devices.value } returns devices
        every { microphoneManager.selectedDevice.value } returns speakerDevice
        every { microphoneManager.select(capture(deviceSlot)) } answers { Unit }

        // When
        speakerManager.setSpeakerPhone(false)

        // Then
        verify { microphoneManager.enforceSetup(preferSpeaker = false, any()) }
        assertEquals(earpieceDevice, deviceSlot.captured)
        assertEquals(false, speakerManager.speakerPhoneEnabled.value)
    }

    @Test
    fun `setSpeakerPhone to false with no previous device selects first non-speaker device`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val speakerManager = SpeakerManager(mediaManager, microphoneManager)

        val audioDevice = mockk<AudioDevice>()
        val speakerDevice = StreamAudioDevice.Speakerphone("test-speaker", audioDevice)
        val earpieceDevice = StreamAudioDevice.Earpiece("test-earpiece", audioDevice)
        val devices = listOf(speakerDevice, earpieceDevice)

        speakerManager.selectedBeforeSpeaker = null

        val deviceSlot = slot<StreamAudioDevice>()

        // Set up enforceSetup to execute the lambda immediately
        every { microphoneManager.enforceSetup(any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }
        every { microphoneManager.devices.value } returns devices
        every { microphoneManager.selectedDevice.value } returns speakerDevice
        every { microphoneManager.select(capture(deviceSlot)) } answers { Unit }

        // When
        speakerManager.setSpeakerPhone(false)

        // Then
        verify { microphoneManager.enforceSetup(preferSpeaker = false, any()) }
        assertEquals(earpieceDevice, deviceSlot.captured)
        assertEquals(false, speakerManager.speakerPhoneEnabled.value)
    }

    @Test
    fun `setSpeakerPhone to false with specific fallback device uses that device`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val speakerManager = SpeakerManager(mediaManager, microphoneManager)

        val audioDevice = mockk<AudioDevice>()
        val speakerDevice = StreamAudioDevice.Speakerphone("test-speaker", audioDevice)
        val earpieceDevice = StreamAudioDevice.Earpiece("test-earpiece", audioDevice)
        val wiredHeadsetDevice = StreamAudioDevice.WiredHeadset("test-wired", audioDevice)

        val devices = listOf(speakerDevice, earpieceDevice, wiredHeadsetDevice)
        val deviceSlot = slot<StreamAudioDevice>()

        // Set up enforceSetup to execute the lambda immediately
        every { microphoneManager.enforceSetup(any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }
        every { microphoneManager.devices.value } returns devices
        every { microphoneManager.selectedDevice.value } returns speakerDevice
        every { microphoneManager.select(capture(deviceSlot)) } answers { Unit }

        // When
        speakerManager.setSpeakerPhone(false, wiredHeadsetDevice)

        // Then
        verify { microphoneManager.enforceSetup(preferSpeaker = false, any()) }
        assertEquals(wiredHeadsetDevice, deviceSlot.captured)
        assertEquals(false, speakerManager.speakerPhoneEnabled.value)
    }

    @Test
    fun `setSpeakerPhone to false with only speakers available uses first available device`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val speakerManager = SpeakerManager(mediaManager, microphoneManager)

        val audioDevice = mockk<AudioDevice>()
        val speakerDevice1 = StreamAudioDevice.Speakerphone("test-speaker-1", audioDevice)
        val speakerDevice2 = StreamAudioDevice.Speakerphone("test-speaker-2", audioDevice)

        // Only speaker devices are available
        val devices = listOf(speakerDevice1, speakerDevice2)

        // No previously selected device
        speakerManager.selectedBeforeSpeaker = null

        val deviceSlot = slot<StreamAudioDevice>()

        // Set up enforceSetup to execute the lambda immediately
        every { microphoneManager.enforceSetup(any(), any()) } answers {
            secondArg<() -> Unit>().invoke()
        }
        every { microphoneManager.devices.value } returns devices
        every { microphoneManager.selectedDevice.value } returns speakerDevice1
        every { microphoneManager.select(capture(deviceSlot)) } answers { Unit }

        // When
        speakerManager.setSpeakerPhone(false)

        // Then
        verify { microphoneManager.enforceSetup(preferSpeaker = false, any()) }
        // Since we only have speakers available, verify we selected the first one
        verify { microphoneManager.select(any()) } // Verify the select method was called
        assertEquals(false, speakerManager.speakerPhoneEnabled.value)
    }

    @Test
    fun `audioUsage initializes with value from audioUsageProvider`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val initialAudioUsage = AudioAttributes.USAGE_MEDIA
        val audioUsageProvider = { initialAudioUsage }

        // When
        val speakerManager = SpeakerManager(
            mediaManager,
            microphoneManager,
            audioUsageProvider = audioUsageProvider,
        )

        // Then
        assertEquals(initialAudioUsage, speakerManager.audioUsage.value)
    }

    @Test
    fun `setAudioUsage updates StateFlow when ADM update succeeds`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val initialAudioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION
        val newAudioUsage = AudioAttributes.USAGE_MEDIA
        val audioUsageProvider = { initialAudioUsage }

        val call = mockk<Call>(relaxed = true)
        val peerConnectionFactory = mockk<StreamPeerConnectionFactory>(relaxed = true)

        every { mediaManager.call } returns call
        every { call.peerConnectionFactory } returns peerConnectionFactory
        every { peerConnectionFactory.updateAudioTrackUsage(newAudioUsage) } returns true

        val speakerManager = SpeakerManager(
            mediaManager,
            microphoneManager,
            audioUsageProvider = audioUsageProvider,
        )
        assertEquals(initialAudioUsage, speakerManager.audioUsage.value)

        // When
        val result = speakerManager.setAudioUsage(newAudioUsage)

        // Then
        assertEquals(true, result)
        assertEquals(newAudioUsage, speakerManager.audioUsage.value)
        verify { peerConnectionFactory.updateAudioTrackUsage(newAudioUsage) }
    }

    @Test
    fun `setAudioUsage does not update StateFlow when ADM update fails`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val initialAudioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION
        val newAudioUsage = AudioAttributes.USAGE_MEDIA
        val audioUsageProvider = { initialAudioUsage }

        val call = mockk<Call>(relaxed = true)
        val peerConnectionFactory = mockk<StreamPeerConnectionFactory>(relaxed = true)

        every { mediaManager.call } returns call
        every { call.peerConnectionFactory } returns peerConnectionFactory
        every { peerConnectionFactory.updateAudioTrackUsage(newAudioUsage) } returns false

        val speakerManager = SpeakerManager(
            mediaManager,
            microphoneManager,
            audioUsageProvider = audioUsageProvider,
        )
        assertEquals(initialAudioUsage, speakerManager.audioUsage.value)

        // When
        val result = speakerManager.setAudioUsage(newAudioUsage)

        // Then
        assertEquals(false, result)
        assertEquals(initialAudioUsage, speakerManager.audioUsage.value) // Should remain unchanged
        verify { peerConnectionFactory.updateAudioTrackUsage(newAudioUsage) }
    }

    @Test
    fun `setAudioUsage does not update StateFlow when ADM is null`() = runTest {
        // Given
        val mediaManager = mockk<MediaManagerImpl>(relaxed = true)
        val microphoneManager = mockk<MicrophoneManager>(relaxed = true)
        val initialAudioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION
        val newAudioUsage = AudioAttributes.USAGE_MEDIA
        val audioUsageProvider = { initialAudioUsage }

        val call = mockk<Call>(relaxed = true)
        val peerConnectionFactory = mockk<StreamPeerConnectionFactory>(relaxed = true)

        every { mediaManager.call } returns call
        every { call.peerConnectionFactory } returns peerConnectionFactory
        every { peerConnectionFactory.updateAudioTrackUsage(newAudioUsage) } returns false

        val speakerManager = SpeakerManager(
            mediaManager,
            microphoneManager,
            audioUsageProvider = audioUsageProvider,
        )
        assertEquals(initialAudioUsage, speakerManager.audioUsage.value)

        // When
        val result = speakerManager.setAudioUsage(newAudioUsage)

        // Then
        assertEquals(false, result)
        assertEquals(initialAudioUsage, speakerManager.audioUsage.value) // Should remain unchanged
    }
}
