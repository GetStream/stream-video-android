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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.getstream.log.taggedLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Listener interface for audio device changes using NativeStreamAudioDevice.
 */
internal typealias CustomAudioDeviceChangeListener = (
    devices: List<CustomAudioDevice>,
    selectedDevice: CustomAudioDevice?,
) -> Unit

/**
 * Custom AudioSwitch implementation using Android's AudioManager APIs.
 * Replaces Twilio's AudioSwitch library with native Android functionality.
 * Uses NativeStreamAudioDevice for all device operations.
 */
internal class StreamAudioSwitch(
    private val context: Context,
    preferredDeviceList: List<Class<out CustomAudioDevice>>? = null,
) {
    private val logger by taggedLogger(TAG)

    private val preferredDeviceList: List<Class<out CustomAudioDevice>> =
        preferredDeviceList ?: getDefaultPreferredDeviceList()

    private val audioManager: AudioManager = context.getSystemService(
        Context.AUDIO_SERVICE,
    ) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var audioDeviceChangeListener: CustomAudioDeviceChangeListener? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var isStarted = false

    // Device manager - selected based on SDK version in start()
    private var deviceManager: AudioDeviceManager? = null

    // Audio focus management
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val typeOfChange: String = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> "AUDIOFOCUS_GAIN"
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> "AUDIOFOCUS_GAIN_TRANSIENT"
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE"
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK"
            AudioManager.AUDIOFOCUS_LOSS -> "AUDIOFOCUS_LOSS"
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "AUDIOFOCUS_LOSS_TRANSIENT"
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
            else -> "AUDIOFOCUS_INVALID"
        }
        logger.d { "[onAudioFocusChange] focusChange: $typeOfChange" }
    }

    private val _availableDevices = MutableStateFlow<List<CustomAudioDevice>>(emptyList())
    public val availableDevices: StateFlow<List<CustomAudioDevice>> = _availableDevices.asStateFlow()

    private val _selectedDeviceState = MutableStateFlow<CustomAudioDevice?>(null)
    public val selectedDeviceState: StateFlow<CustomAudioDevice?> = _selectedDeviceState.asStateFlow()

    // Track previous device before Bluetooth was selected (for fallback on Bluetooth failure)
    private var previousDeviceBeforeBluetooth: CustomAudioDevice? = null

    /**
     * Starts monitoring audio devices and begins device enumeration.
     * @param listener Callback that receives device updates
     */
    public fun start(listener: CustomAudioDeviceChangeListener? = null) {
        synchronized(this) {
            if (isStarted) {
                logger.w { "[start] AudioSwitch already started" }
                return
            }

            audioDeviceChangeListener = listener
            isStarted = true

            logger.d { "[start] Starting AudioSwitch" }

            // Create appropriate device manager based on SDK version
            deviceManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                logger.d { "[start] Using ModernAudioDeviceManager (API 31+)" }
                ModernAudioDeviceManager(audioManager)
            } else {
                logger.d { "[start] Using LegacyAudioDeviceManager (API 24-30)" }
                LegacyAudioDeviceManager(
                    context = context,
                    audioManager = audioManager,
                    onDeviceChange = { enumerateDevices() },
                    onBluetoothConnectionFailure = { handleBluetoothConnectionFailure() },
                )
            }

            // Request audio focus for voice communication
            requestAudioFocus()

            // Start the device manager
            deviceManager?.start()

            // Register device callback (always available since minSdk is 24)
            registerDeviceCallback()

            // Initial device enumeration
            enumerateDevices()
        }
    }

    /**
     * Stops monitoring audio devices and releases resources.
     */
    public fun stop() {
        synchronized(this) {
            if (!isStarted) {
                logger.w { "[stop] AudioSwitch not started" }
                return
            }

            logger.d { "[stop] Stopping AudioSwitch" }

            // Deactivate audio routing (matches Twilio behavior)
            deactivate()

            // Abandon audio focus
            abandonAudioFocus()

            // Unregister device callback
            unregisterDeviceCallback()

            // Stop and clear device manager
            deviceManager?.stop()
            deviceManager = null

            audioDeviceChangeListener = null
            isStarted = false
            _availableDevices.value = emptyList()
            _selectedDeviceState.value = null
            previousDeviceBeforeBluetooth = null
        }
    }

    /**
     * Selects an audio device for routing.
     * @param device The device to select, or null for automatic selection
     */
    public fun selectDevice(device: CustomAudioDevice?) {
        selectCustomAudioDevice(device)
    }

    /**
     * Deactivates audio routing.
     */
    public fun deactivate() {
        synchronized(this) {
            logger.d { "[deactivate] Deactivating audio routing" }
            // Note: Audio focus is managed by WebRTC, not by device switching
        }
    }

    /**
     * Gets the currently selected device.
     */
    public fun getSelectedDevice(): CustomAudioDevice? = _selectedDeviceState.value

    /**
     * Gets the list of available devices.
     */
    public fun getAvailableDevices(): List<CustomAudioDevice> = _availableDevices.value

    /**
     * Selects a native audio device for routing.
     * @param device The native device to select, or null for automatic selection
     */
    public fun selectCustomAudioDevice(device: CustomAudioDevice?) {
        synchronized(this) {
            if (!isStarted) {
                logger.w { "[selectCustomAudioDevice] AudioSwitch not started" }
                return
            }

            logger.i { "[selectCustomAudioDevice] Selecting native device: $device" }

            val deviceToSelect = device ?: selectCustomDeviceByPriority()
            val currentSelected = _selectedDeviceState.value

            if (deviceToSelect != null) {
                val manager = deviceManager
                if (manager != null) {
                    // If switching to Bluetooth, save the previous device for fallback
                    if (deviceToSelect is CustomAudioDevice.BluetoothHeadset &&
                        currentSelected != null &&
                        currentSelected !is CustomAudioDevice.BluetoothHeadset
                    ) {
                        previousDeviceBeforeBluetooth = currentSelected
                        logger.d {
                            "[selectNativeDevice] Saving previous device before Bluetooth: $previousDeviceBeforeBluetooth"
                        }
                    }

                    val success = manager.selectDevice(deviceToSelect)
                    if (success) {
                        _selectedDeviceState.value = deviceToSelect
                        logger.d { "[selectNativeDevice] Native device selected: $deviceToSelect" }
                    } else {
                        logger.w { "[selectNativeDevice] Failed to select native device: $deviceToSelect" }
                        // Fallback to automatic selection if the requested device is not available
                        val autoSelected = selectCustomDeviceByPriority()
                        if (autoSelected != null && autoSelected != deviceToSelect) {
                            selectCustomAudioDevice(autoSelected)
                        }
                    }
                } else {
                    logger.w { "[selectNativeDevice] Device manager not initialized" }
                }
            } else {
                logger.w { "[selectNativeDevice] No device available to select" }
                deviceManager?.clearDevice()
                _selectedDeviceState.value = null
            }
        }
    }

    /**
     * Selects a device by priority from available native devices.
     */
    private fun selectCustomDeviceByPriority(): CustomAudioDevice? {
        val availableDevices = _availableDevices.value
        if (availableDevices.isEmpty()) {
            return null
        }

        // Select device based on preferredDeviceList priority
        for (preferredType in preferredDeviceList) {
            val device = availableDevices.find { preferredType.isInstance(it) }
            if (device != null) {
                return device
            }
        }

        // Fallback to first available device
        return availableDevices.firstOrNull()
    }

    private fun registerDeviceCallback() {
        if (audioDeviceCallback != null) {
            return
        }

        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                logger.d { "[onAudioDevicesAdded] Devices added: ${addedDevices.size}" }
                enumerateDevices()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                logger.d { "[onAudioDevicesRemoved] Devices removed: ${removedDevices.size}" }
                enumerateDevices()
            }
        }

        StreamAudioManager.registerAudioDeviceCallback(
            audioManager,
            audioDeviceCallback!!,
            mainHandler,
        )
    }

    private fun unregisterDeviceCallback() {
        audioDeviceCallback?.let { callback ->
            StreamAudioManager.unregisterAudioDeviceCallback(audioManager, callback)
            audioDeviceCallback = null
        }
    }

    private fun enumerateDevices() {
        // Enumerate devices using the device manager (which works with NativeStreamAudioDevice)
        val manager = deviceManager
        val nativeDevices = manager?.enumerateDevices() ?: emptyList()

        _availableDevices.value = nativeDevices

        // Update selected device
        val currentSelected = _selectedDeviceState.value
        if (currentSelected != null && !nativeDevices.contains(currentSelected)) {
            logger.w { "[enumerateDevices] Selected device no longer available: $currentSelected" }
            deviceManager?.clearDevice()
            _selectedDeviceState.value = null
        } else {
            // Update from manager
            val managerSelected = manager?.getSelectedDevice()
            if (managerSelected != null) {
                _selectedDeviceState.value = managerSelected
            }
        }

        // Notify listener
        audioDeviceChangeListener?.invoke(nativeDevices, _selectedDeviceState.value)
    }

    /**
     * Handles Bluetooth connection failure by reverting to the previously selected device.
     */
    private fun handleBluetoothConnectionFailure() {
        logger.w {
            "[handleBluetoothConnectionFailure] Bluetooth connection failed, reverting to previous device"
        }

        val availableDevices = _availableDevices.value

        // First, try to revert to the device that was selected before Bluetooth
        val previousDevice = previousDeviceBeforeBluetooth
        if (previousDevice != null) {
            if (availableDevices.contains(previousDevice)) {
                logger.d { "[handleBluetoothConnectionFailure] Reverting to previous device: $previousDevice" }
                previousDeviceBeforeBluetooth = null // Clear after use
                selectCustomAudioDevice(previousDevice)
                return
            } else {
                logger.d {
                    "[handleBluetoothConnectionFailure] Previous device no longer available: $previousDeviceBeforeBluetooth"
                }
                previousDeviceBeforeBluetooth = null
            }
        }

        // Fallback to automatic selection by priority if no previous device
        logger.d { "[handleBluetoothConnectionFailure] No previous device, using automatic selection" }
        val autoSelected = selectCustomDeviceByPriority()
        if (autoSelected != null) {
            selectCustomAudioDevice(autoSelected)
        }
    }

    /**
     * Requests audio focus for audio routing.
     * Uses AudioFocusRequest on API 26+ and legacy requestAudioFocus on older versions.
     */
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use generic audio attributes - the actual usage will be set by AudioTrack
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                val result = audioManager.requestAudioFocus(focusRequest)
                audioFocusRequest = focusRequest

                logger.d { "[requestAudioFocus] Audio focus request result: $result" }
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    logger.d { "[requestAudioFocus] Audio focus granted" }
                } else {
                    logger.w { "[requestAudioFocus] Audio focus not granted, result: $result" }
                }
            } else {
                // Legacy API - uses stream type instead of usage
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                )
                logger.d { "[requestAudioFocus] Audio focus request result: $result" }
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    logger.d { "[requestAudioFocus] Audio focus granted" }
                } else {
                    logger.w { "[requestAudioFocus] Audio focus not granted, result: $result" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "[requestAudioFocus] Error requesting audio focus: ${e.message}" }
        }
    }

    /**
     * Abandons audio focus.
     */
    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    val result = audioManager.abandonAudioFocusRequest(request)
                    logger.d { "[abandonAudioFocus] Audio focus abandoned, result: $result" }
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.abandonAudioFocus(audioFocusChangeListener)
                logger.d { "[abandonAudioFocus] Audio focus abandoned, result: $result" }
            }
        } catch (e: Exception) {
            logger.e(e) { "[abandonAudioFocus] Error abandoning audio focus: ${e.message}" }
        }
    }

    public companion object {
        private const val TAG = "StreamAudioSwitch"

        /**
         * Returns the default preferred device list matching Twilio's priority:
         * BluetoothHeadset -> WiredHeadset -> Earpiece -> Speakerphone
         */
        @JvmStatic
        fun getDefaultPreferredDeviceList(): List<Class<out CustomAudioDevice>> {
            return listOf(
                CustomAudioDevice.BluetoothHeadset::class.java,
                CustomAudioDevice.WiredHeadset::class.java,
                CustomAudioDevice.Earpiece::class.java,
                CustomAudioDevice.Speakerphone::class.java,
            )
        }
    }
}
