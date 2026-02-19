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

package io.getstream.video.android.core.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModernAudioDeviceManagerTest {

    private lateinit var audioManager: AudioManager

    private lateinit var manager: ModernAudioDeviceManager

    @Before
    fun setUp() {
        audioManager = mockk<AudioManager>(relaxed = true)
        manager = ModernAudioDeviceManager(audioManager)
        mockkObject(StreamAudioManager)
        // Default mock for getCommunicationDevice to avoid NoSuchMethodError
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null
    }

    @org.junit.After
    fun tearDown() {
        unmockkObject(StreamAudioManager)
    }

    @Test
    fun `enumerateDevices returns empty list when no devices available`() {
        // Given
        every { StreamAudioManager.getAvailableCommunicationDevices(audioManager) } returns emptyList()

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).isEmpty()
    }

    @Test
    fun `enumerateDevices includes speakerphone when available`() {
        // Given
        val speakerDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { speakerDevice.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerDevice.productName } returns "Speaker"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(speakerDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.Speakerphone::class.java)
    }

    @Test
    fun `enumerateDevices includes earpiece when available`() {
        // Given
        val earpieceDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { earpieceDevice.type } returns AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        every { earpieceDevice.productName } returns "Earpiece"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(earpieceDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.Earpiece::class.java)
    }

    @Test
    fun `enumerateDevices includes wired headset when available`() {
        // Given
        val wiredHeadsetDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { wiredHeadsetDevice.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { wiredHeadsetDevice.productName } returns "Wired Headset"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(wiredHeadsetDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.WiredHeadset::class.java)
    }

    @Test
    fun `enumerateDevices includes Bluetooth headset when available`() {
        // Given
        val bluetoothDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { bluetoothDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothDevice.productName } returns "Bluetooth Headset"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(bluetoothDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.BluetoothHeadset::class.java)
    }

    @Test
    fun `enumerateDevices includes multiple device types`() {
        // Given
        val speakerDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { speakerDevice.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerDevice.productName } returns "Speaker"

        val earpieceDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { earpieceDevice.type } returns AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        every { earpieceDevice.productName } returns "Earpiece"

        val wiredHeadsetDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { wiredHeadsetDevice.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { wiredHeadsetDevice.productName } returns "Wired Headset"

        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(speakerDevice, earpieceDevice, wiredHeadsetDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(3)
        assertThat(devices.any { it is StreamAudioDevice.Speakerphone }).isTrue()
        assertThat(devices.any { it is StreamAudioDevice.Earpiece }).isTrue()
        assertThat(devices.any { it is StreamAudioDevice.WiredHeadset }).isTrue()
    }

    @Test
    fun `enumerateDevices filters out unsupported device types`() {
        // Given
        val unsupportedDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { unsupportedDevice.type } returns AudioDeviceInfo.TYPE_HDMI
        every { unsupportedDevice.productName } returns "HDMI Device"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(unsupportedDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).isEmpty()
    }

    @Test
    fun `enumerateDevices handles exception when getting available devices`() {
        // Given
        // StreamAudioManager.getAvailableCommunicationDevices catches exceptions internally
        // and returns emptyList(), so we mock it to return emptyList to simulate the exception case
        every { StreamAudioManager.getAvailableCommunicationDevices(audioManager) } returns emptyList()

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).isEmpty()
    }

    @Test
    fun `selectDevice succeeds when device has audioDeviceInfo`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isTrue()
        verify { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) }
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }

    @Test
    fun `selectDevice uses toAudioDeviceInfo when device has no audioDeviceInfo`() {
        // Given
        val device = StreamAudioDevice.Speakerphone()
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { audioDeviceInfo.id } returns 1
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(audioDeviceInfo)
        // Mock fallback for Speakerphone if not found in communication devices
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }
        // Mock getCommunicationDevice to return null so it uses the stored selectedDevice
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isTrue()
        verify { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) }
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }

    @Test
    fun `selectDevice fails when setCommunicationDevice returns false`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { false }
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isFalse()
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `selectDevice fails when toAudioDeviceInfo returns null`() {
        // Given
        val device = StreamAudioDevice.Speakerphone()
        every { StreamAudioManager.getAvailableCommunicationDevices(audioManager) } returns emptyList()

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isFalse()
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `selectDevice handles exception when setting communication device`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { false }

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isFalse()
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `clearDevice clears communication device and resets selected device`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }
        every { StreamAudioManager.clearCommunicationDevice(audioManager) } answers { true }
        manager.selectDevice(device)

        // When
        manager.clearDevice()

        // Then
        verify { StreamAudioManager.clearCommunicationDevice(audioManager) }
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `clearDevice handles exception when clearing communication device`() {
        // Given
        // Mock clearCommunicationDevice to return false (simulating failure)
        // Using answers to prevent real implementation from executing
        every { StreamAudioManager.clearCommunicationDevice(audioManager) } answers { false }
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null

        // When
        manager.clearDevice()

        // Then
        // Should not crash - the implementation handles the failure gracefully
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `getSelectedDevice returns null when no device is selected`() {
        // When
        val device = manager.getSelectedDevice()

        // Then
        assertThat(device).isNull()
    }

    @Test
    fun `getSelectedDevice returns selected device`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }
        // Mock getCommunicationDevice to return null so it uses the stored selectedDevice
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null

        // When
        manager.selectDevice(device)
        val selectedDevice = manager.getSelectedDevice()

        // Then
        assertThat(selectedDevice).isEqualTo(device)
    }

    @Test
    fun `getSelectedDevice gets device from AudioManager when available`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { audioDeviceInfo.productName } returns "Speaker"
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns audioDeviceInfo

        // When
        val selectedDevice = manager.getSelectedDevice()

        // Then
        assertThat(selectedDevice).isNotNull()
        assertThat(selectedDevice).isInstanceOf(StreamAudioDevice.Speakerphone::class.java)
    }

    @Test
    fun `getSelectedDevice returns stored device when AudioManager returns null`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null
        manager.selectDevice(device)

        // When
        val selectedDevice = manager.getSelectedDevice()

        // Then
        assertThat(selectedDevice).isEqualTo(device)
    }

    @Test
    fun `getSelectedDevice handles exception when getting communication device`() {
        // Given
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null

        // When
        val device = manager.getSelectedDevice()

        // Then
        // Should not crash and return null
        assertThat(device).isNull()
    }

    @Test
    fun `getSelectedDevice returns null when AudioManager device cannot be converted`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_HDMI // Unsupported type
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns audioDeviceInfo

        // When
        val selectedDevice = manager.getSelectedDevice()

        // Then
        assertThat(selectedDevice).isNull()
    }

    @Test
    fun `start does nothing for modern API`() {
        // When
        manager.start()

        // Then
        // Should not crash - no special setup needed
    }

    @Test
    fun `stop clears device`() {
        // Given
        val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
        every { audioDeviceInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val device = StreamAudioDevice.Speakerphone(audioDeviceInfo = audioDeviceInfo)
        every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }
        every { StreamAudioManager.clearCommunicationDevice(audioManager) } answers { true }
        every { StreamAudioManager.getCommunicationDevice(audioManager) } returns null
        manager.selectDevice(device)

        // When
        manager.stop()

        // Then
        verify { StreamAudioManager.clearCommunicationDevice(audioManager) }
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `enumerateDevices handles USB headset type`() {
        // Given
        val usbHeadsetDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { usbHeadsetDevice.type } returns AudioDeviceInfo.TYPE_USB_HEADSET
        every { usbHeadsetDevice.productName } returns "USB Headset"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(usbHeadsetDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.WiredHeadset::class.java)
    }

    @Test
    fun `enumerateDevices handles wired headphones type`() {
        // Given
        val wiredHeadphonesDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { wiredHeadphonesDevice.type } returns AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        every { wiredHeadphonesDevice.productName } returns "Wired Headphones"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(wiredHeadphonesDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.WiredHeadset::class.java)
    }

    @Test
    fun `enumerateDevices handles Bluetooth A2DP type`() {
        // Given
        val bluetoothA2dpDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { bluetoothA2dpDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        every { bluetoothA2dpDevice.productName } returns "Bluetooth A2DP"
        every {
            StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        } returns listOf(bluetoothA2dpDevice)

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1)
        assertThat(devices.first()).isInstanceOf(StreamAudioDevice.BluetoothHeadset::class.java)
    }

    @Test
    fun `selectDevice handles different device types correctly`() {
        // Test each device type
        val testCases = listOf(
            Pair(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, StreamAudioDevice.Speakerphone()),
            Pair(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, StreamAudioDevice.Earpiece()),
            Pair(AudioDeviceInfo.TYPE_WIRED_HEADSET, StreamAudioDevice.WiredHeadset()),
            Pair(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, StreamAudioDevice.BluetoothHeadset()),
        )

        testCases.forEach { (deviceType, device) ->
            // Given
            val audioDeviceInfo = mockk<AudioDeviceInfo>(relaxed = true)
            every { audioDeviceInfo.type } returns deviceType
            val deviceWithInfo = when (device) {
                is StreamAudioDevice.Speakerphone -> StreamAudioDevice.Speakerphone(
                    audioDeviceInfo = audioDeviceInfo,
                )
                is StreamAudioDevice.Earpiece -> StreamAudioDevice.Earpiece(
                    audioDeviceInfo = audioDeviceInfo,
                )
                is StreamAudioDevice.WiredHeadset -> StreamAudioDevice.WiredHeadset(
                    audioDeviceInfo = audioDeviceInfo,
                )
                is StreamAudioDevice.BluetoothHeadset -> StreamAudioDevice.BluetoothHeadset(
                    audioDeviceInfo = audioDeviceInfo,
                )
            }
            every { StreamAudioManager.setCommunicationDevice(audioManager, audioDeviceInfo) } answers { true }

            // When
            val result = manager.selectDevice(deviceWithInfo)

            // Then
            assertThat(result).isTrue()
            assertThat(manager.getSelectedDevice()).isEqualTo(deviceWithInfo)
        }
    }
}
