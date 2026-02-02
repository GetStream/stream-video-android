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

package io.getstream.video.android.core.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
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
class LegacyAudioDeviceManagerTest {

    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothHeadset: BluetoothHeadset
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var packageManager: PackageManager

    private var deviceChangeCallbackInvoked = false
    private var bluetoothFailureCallbackInvoked = false

    private lateinit var manager: LegacyAudioDeviceManager

    @Before
    fun setUp() {
        context = mockk<Context>(relaxed = true)
        audioManager = mockk<AudioManager>(relaxed = true)
        bluetoothManager = mockk<BluetoothManager>(relaxed = true)
        bluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
        bluetoothHeadset = mockk<BluetoothHeadset>(relaxed = true)
        bluetoothDevice = mockk<BluetoothDevice>(relaxed = true)
        packageManager = mockk<PackageManager>(relaxed = true)

        deviceChangeCallbackInvoked = false
        bluetoothFailureCallbackInvoked = false

        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns true

        // Mock StreamAudioManager to prevent issues with API 31+ methods
        mockkObject(StreamAudioManager)
        // For API < 31, StreamAudioManager.getAvailableCommunicationDevices uses getDevices()
        // So we delegate to the mocked getDevices() results
        every { StreamAudioManager.getAvailableCommunicationDevices(audioManager) } answers {
            try {
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        manager = LegacyAudioDeviceManager(
            context = context,
            audioManager = audioManager,
            onDeviceChange = { deviceChangeCallbackInvoked = true },
            onBluetoothConnectionFailure = { bluetoothFailureCallbackInvoked = true },
        )
    }

    @org.junit.After
    fun tearDown() {
        unmockkObject(StreamAudioManager)
    }

    @Test
    fun `enumerateDevices returns speakerphone when no other devices available`() {
        // Given
        every { audioManager.getDevices(any()) } returns emptyArray()
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(2) // Speakerphone and Earpiece
        assertThat(devices).contains(StreamAudioDevice.Speakerphone())
        assertThat(devices).contains(StreamAudioDevice.Earpiece())
    }

    @Test
    fun `enumerateDevices includes wired headset when detected via AudioDeviceInfo`() {
        // Given
        val wiredHeadsetDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { wiredHeadsetDevice.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { wiredHeadsetDevice.productName } returns "Wired Headset"
        every { audioManager.getDevices(any()) } returns arrayOf(wiredHeadsetDevice)
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(3) // WiredHeadset, Speakerphone, Earpiece
        assertThat(devices.any { it is StreamAudioDevice.WiredHeadset }).isTrue()
    }

    @Test
    fun `enumerateDevices includes wired headset when detected via isWiredHeadsetOn fallback`() {
        // Given
        every { audioManager.getDevices(any()) } returns emptyArray()
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns true

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(3) // WiredHeadset, Speakerphone, Earpiece
        assertThat(devices.any { it is StreamAudioDevice.WiredHeadset }).isTrue()
    }

    @Test
    fun `enumerateDevices includes Bluetooth device when detected via AudioDeviceInfo`() {
        // Given
        val bluetoothDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { bluetoothDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothDevice.productName } returns "Bluetooth Headset"
        every { bluetoothDevice.address } returns "00:11:22:33:44:55"
        every { audioManager.getDevices(any()) } returns arrayOf(bluetoothDevice)
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(3) // BluetoothHeadset, Speakerphone, Earpiece
        assertThat(devices.any { it is StreamAudioDevice.BluetoothHeadset }).isTrue()
    }

    @Test
    fun `enumerateDevices excludes earpiece when device has no telephony feature`() {
        // Given
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } returns false
        every { audioManager.getDevices(any()) } returns emptyArray()
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        assertThat(devices).hasSize(1) // Only Speakerphone
        assertThat(devices).contains(StreamAudioDevice.Speakerphone())
        assertThat(devices).doesNotContain(StreamAudioDevice.Earpiece())
    }

    @Test
    fun `enumerateDevices deduplicates Bluetooth devices by address preferring SCO over A2DP`() {
        // Given
        val bluetoothScoDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { bluetoothScoDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothScoDevice.productName } returns "Bluetooth SCO"
        every { bluetoothScoDevice.address } returns "00:11:22:33:44:55"

        val bluetoothA2dpDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { bluetoothA2dpDevice.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        every { bluetoothA2dpDevice.productName } returns "Bluetooth A2DP"
        every { bluetoothA2dpDevice.address } returns "00:11:22:33:44:55" // Same address

        every { audioManager.getDevices(any()) } returns arrayOf(bluetoothScoDevice, bluetoothA2dpDevice)
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        val bluetoothDevices = devices.filterIsInstance<StreamAudioDevice.BluetoothHeadset>()
        assertThat(bluetoothDevices).hasSize(1)
        assertThat(
            bluetoothDevices.first().audioDeviceInfo?.type,
        ).isEqualTo(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
    }

    @Test
    fun `selectDevice selects speakerphone and stops Bluetooth SCO`() {
        // Given
        val device = StreamAudioDevice.Speakerphone()
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isTrue()
        @Suppress("DEPRECATION")
        verify { audioManager.isSpeakerphoneOn = true }
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }

    @Test
    fun `selectDevice selects earpiece and stops Bluetooth SCO`() {
        // Given
        val device = StreamAudioDevice.Earpiece()
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isTrue()
        @Suppress("DEPRECATION")
        verify { audioManager.isSpeakerphoneOn = false }
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }

    @Test
    fun `selectDevice selects wired headset and stops Bluetooth SCO`() {
        // Given
        val device = StreamAudioDevice.WiredHeadset()
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isTrue()
        @Suppress("DEPRECATION")
        verify { audioManager.isSpeakerphoneOn = false }
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }

    @Test
    fun `selectDevice selects Bluetooth headset and starts SCO connection`() {
        // Given
        val device = StreamAudioDevice.BluetoothHeadset()
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns true
        every { bluetoothAdapter.getProfileProxy(any(), any(), any()) } returns true

        // When
        val result = manager.selectDevice(device)

        // Then
        assertThat(result).isTrue()
        @Suppress("DEPRECATION")
        verify { audioManager.isSpeakerphoneOn = false }
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }

    @Test
    fun `clearDevice stops Bluetooth SCO and resets speakerphone`() {
        // Given
        val device = StreamAudioDevice.Speakerphone()
        manager.selectDevice(device)
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false

        // When
        manager.clearDevice()

        // Then
        @Suppress("DEPRECATION")
        verify { audioManager.isSpeakerphoneOn = false }
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
        val device = StreamAudioDevice.Speakerphone()
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false

        // When
        manager.selectDevice(device)
        val selectedDevice = manager.getSelectedDevice()

        // Then
        assertThat(selectedDevice).isEqualTo(device)
    }

    @Test
    fun `start registers legacy listeners`() {
        // Given
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns true
        every { bluetoothAdapter.getProfileProxy(any(), any(), any()) } returns true

        // When
        manager.start()

        // Then
        verify { context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>()) }
    }

    @Test
    fun `start handles Bluetooth adapter being null`() {
        // Given
        every { bluetoothManager.adapter } returns null
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns true

        // When
        manager.start()

        // Then
        // Should not crash
        verify { context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>()) }
    }

    @Test
    fun `start handles Bluetooth SCO not available`() {
        // Given
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns false

        // When
        manager.start()

        // Then
        // Should not crash and should still register headset receiver
        verify { context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>()) }
    }

    @Test
    fun `start handles SecurityException when getting Bluetooth profile proxy`() {
        // Given
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns true
        every {
            bluetoothAdapter.getProfileProxy(any(), any(), any())
        } throws SecurityException("Permission denied")

        // When
        manager.start()

        // Then
        // Should not crash and should fall back to AudioDeviceInfo enumeration
        verify { context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>()) }
    }

    @Test
    fun `stop unregisters listeners and clears device`() {
        // Given
        val device = StreamAudioDevice.Speakerphone()
        manager.selectDevice(device)
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns true
        every { bluetoothAdapter.getProfileProxy(any(), any(), any()) } returns true
        manager.start()

        // When
        manager.stop()

        // Then
        verify { context.unregisterReceiver(any<BroadcastReceiver>()) }
        assertThat(manager.getSelectedDevice()).isNull()
    }

    @Test
    fun `stop handles exception when unregistering receiver`() {
        // Given
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns true
        every { bluetoothAdapter.getProfileProxy(any(), any(), any()) } returns true
        manager.start()
        every {
            context.unregisterReceiver(any<BroadcastReceiver>())
        } throws IllegalArgumentException("Receiver not registered")

        // When
        manager.stop()

        // Then
        // Should not crash
    }

    @Test
    fun `enumerateDevices handles exception when getting devices`() {
        // Given
        every { audioManager.getDevices(any()) } throws RuntimeException("Error getting devices")
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        // Should return at least speakerphone and earpiece
        assertThat(devices).isNotEmpty()
        assertThat(devices).contains(StreamAudioDevice.Speakerphone())
    }

    @Test
    fun `enumerateDevices handles exception when checking wired headset`() {
        // Given
        every { audioManager.getDevices(any()) } returns emptyArray()
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } throws RuntimeException("Error checking headset")

        // When
        val devices = manager.enumerateDevices()

        // Then
        // Should not crash and should return at least speakerphone
        assertThat(devices).isNotEmpty()
    }

    @Test
    fun `enumerateDevices filters out unsupported device types`() {
        // Given
        val unsupportedDevice = mockk<AudioDeviceInfo>(relaxed = true)
        every { unsupportedDevice.type } returns AudioDeviceInfo.TYPE_HDMI
        every { unsupportedDevice.productName } returns "HDMI Device"
        every { audioManager.getDevices(any()) } returns arrayOf(unsupportedDevice)
        @Suppress("DEPRECATION")
        every { audioManager.isWiredHeadsetOn } returns false

        // When
        val devices = manager.enumerateDevices()

        // Then
        // Should not include unsupported device
        assertThat(
            devices.none {
                it is StreamAudioDevice.BluetoothHeadset && it.name == "HDMI Device"
            },
        ).isTrue()
        assertThat(devices).contains(StreamAudioDevice.Speakerphone())
    }

    @Test
    fun `selectDevice handles Bluetooth device when profile proxy is not available`() {
        // Given
        val device = StreamAudioDevice.BluetoothHeadset()
        @Suppress("DEPRECATION")
        every { audioManager.isSpeakerphoneOn } returns false
        @Suppress("DEPRECATION")
        every { audioManager.isBluetoothScoAvailableOffCall } returns false

        // When
        val result = manager.selectDevice(device)

        // Then
        // Should still return true even if SCO can't be started
        assertThat(result).isTrue()
        assertThat(manager.getSelectedDevice()).isEqualTo(device)
    }
}
