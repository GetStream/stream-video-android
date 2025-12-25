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
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import io.getstream.log.taggedLogger

/**
 * Audio device manager for API 24-30.
 * Uses legacy AudioManager APIs and BluetoothHeadset profile proxy for Bluetooth device detection.
 */
internal class LegacyAudioDeviceManager(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onDeviceChange: () -> Unit,
    private val onBluetoothConnectionFailure: () -> Unit,
) : AudioDeviceManager {

    private val logger by taggedLogger(TAG)

    private var selectedDevice: StreamAudioDevice? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Legacy listener support
    private var headsetPlugReceiver: BroadcastReceiver? = null
    private var bluetoothStateReceiver: BroadcastReceiver? = null
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothProfileProxyAvailable = false

    // Bluetooth SCO management state
    private enum class BluetoothScoState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }
    private var bluetoothScoState = BluetoothScoState.DISCONNECTED
    private var bluetoothScoConnectionAttempts = 0
    private val maxBluetoothScoAttempts = 3
    private val bluetoothScoTimeoutHandler = Handler(Looper.getMainLooper())
    private var bluetoothScoTimeoutRunnable: Runnable? = null

    override fun enumerateDevices(): List<StreamAudioDevice> {
        val devices = mutableListOf<StreamAudioDevice>()

        // Detect Bluetooth devices - use profile proxy if available, otherwise fall back to AudioDeviceInfo
        if (bluetoothProfileProxyAvailable && bluetoothHeadset != null) {
            // Use BluetoothHeadset profile proxy - only shows devices that support SCO
            logger.d {
                "[enumerateDevices] Checking Bluetooth devices via headset profile. bluetoothHeadset is connected"
            }
            val bluetoothDevices = detectBluetoothDevices()
            if (bluetoothDevices.isNotEmpty()) {
                logger.d {
                    "[enumerateDevices] Found ${bluetoothDevices.size} Bluetooth device(s) via headset profile"
                }
                devices.addAll(bluetoothDevices)
            } else {
                logger.d { "[enumerateDevices] No Bluetooth devices found via headset profile" }
            }
        } else {
            logger.d {
                "[enumerateDevices] Profile proxy not available, using AudioDeviceInfo enumeration for Bluetooth"
            }
        }

        // Detect other devices (wired headset, earpiece, speakerphone) and Bluetooth (if profile proxy unavailable) via AudioDeviceInfo
        val androidDevices = StreamAudioManager.getAvailableCommunicationDevices(audioManager)
        logger.d { "[enumerateDevices] Found ${androidDevices.size} available communication devices" }

        // Track Bluetooth devices by address to deduplicate SCO and A2DP for the same device
        val bluetoothDevicesByAddress = mutableMapOf<String, StreamAudioDevice.BluetoothHeadset>()

        for (androidDevice in androidDevices) {
            val streamAudioDevice = StreamAudioDevice.fromAudioDeviceInfo(androidDevice)
            if (streamAudioDevice != null) {
                when (streamAudioDevice) {
                    is StreamAudioDevice.BluetoothHeadset -> {
                        // If profile proxy is available, skip AudioDeviceInfo Bluetooth devices (we already got them from profile)
                        if (bluetoothProfileProxyAvailable && bluetoothHeadset != null) {
                            continue
                        }
                        // Otherwise, use AudioDeviceInfo enumeration and deduplicate by address
                        val address = androidDevice.address ?: ""
                        val existing = bluetoothDevicesByAddress[address]
                        if (existing == null) {
                            logger.d {
                                "[enumerateDevices] Detected Bluetooth device via AudioDeviceInfo: ${streamAudioDevice.name}, type=${androidDevice.type}, address=$address"
                            }
                            bluetoothDevicesByAddress[address] = streamAudioDevice
                        } else {
                            // Prefer SCO over A2DP
                            val isSco = androidDevice.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            val existingIsSco = existing.audioDeviceInfo?.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            if (isSco && !existingIsSco) {
                                logger.d {
                                    "[enumerateDevices] Replacing A2DP with SCO for Bluetooth device: ${streamAudioDevice.name}, address=$address"
                                }
                                bluetoothDevicesByAddress[address] = streamAudioDevice
                            } else {
                                logger.d {
                                    "[enumerateDevices] Skipping duplicate Bluetooth device (keeping ${if (existingIsSco) "SCO" else "A2DP"}): type=${androidDevice.type}, address=$address"
                                }
                            }
                        }
                    }
                    else -> {
                        logger.d {
                            "[enumerateDevices] Detected device: ${streamAudioDevice::class.simpleName} (${streamAudioDevice.name})"
                        }
                        devices.add(streamAudioDevice)
                    }
                }
            } else {
                logger.w {
                    "[enumerateDevices] Could not convert AudioDeviceInfo to StreamAudioDevice: type=${androidDevice.type}, name=${androidDevice.productName}"
                }
            }
        }

        // Add deduplicated Bluetooth devices from AudioDeviceInfo (if profile proxy was not used)
        if (!bluetoothProfileProxyAvailable || bluetoothHeadset == null) {
            devices.addAll(bluetoothDevicesByAddress.values)
        }

        // Check for wired headset using ACTION_HEADSET_PLUG state
        // (getDevices() might not always detect it reliably)
        if (devices.none { it is StreamAudioDevice.WiredHeadset }) {
            @Suppress("DEPRECATION")
            val isWiredHeadsetOn = try {
                audioManager.isWiredHeadsetOn
            } catch (e: Exception) {
                false
            }
            if (isWiredHeadsetOn) {
                logger.d { "[enumerateDevices] Adding WiredHeadset via fallback check (isWiredHeadsetOn=true)" }
                devices.add(StreamAudioDevice.WiredHeadset())
            }
        }

        // Add speakerphone - always available
        if (devices.none { it is StreamAudioDevice.Speakerphone }) {
            logger.d { "[enumerateDevices] Adding Speakerphone (always available)" }
            devices.add(StreamAudioDevice.Speakerphone())
        }

        // Add earpiece only if device has telephony feature (is a phone)
        if (devices.none { it is StreamAudioDevice.Earpiece }) {
            val hasEarpiece = hasEarpiece(context)
            if (hasEarpiece) {
                logger.d { "[enumerateDevices] Adding Earpiece (device has telephony feature)" }
                devices.add(StreamAudioDevice.Earpiece())
            } else {
                logger.d { "[enumerateDevices] Skipping Earpiece (device does not have telephony feature)" }
            }
        }

        logger.d { "[enumerateDevices] Total enumerated devices: ${devices.size}" }
        devices.forEachIndexed { index, device ->
            logger.d { "[enumerateDevices] Final device $index: ${device::class.simpleName} (${device.name})" }
        }

        return devices
    }

    override fun selectDevice(device: StreamAudioDevice): Boolean {
        when (device) {
            is StreamAudioDevice.Speakerphone -> {
                stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
                selectedDevice = device
                return true
            }
            is StreamAudioDevice.Earpiece -> {
                stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                selectedDevice = device
                return true
            }
            is StreamAudioDevice.BluetoothHeadset -> {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                // All Bluetooth devices detected via BluetoothHeadset profile support SCO
                logger.d { "[selectDevice] Bluetooth device selected, starting SCO connection" }
                startBluetoothSco()

                selectedDevice = device
                return true
            }
            is StreamAudioDevice.WiredHeadset -> {
                stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                selectedDevice = device
                return true
            }
        }
    }

    override fun clearDevice() {
        stopBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
        selectedDevice = null
    }

    override fun getSelectedDevice(): StreamAudioDevice? = selectedDevice

    override fun start() {
        registerLegacyListeners()
    }

    override fun stop() {
        unregisterLegacyListeners()
        stopBluetoothSco()
        clearDevice()
    }

    /**
     * Registers legacy listeners for API 24-30:
     * - BroadcastReceiver for ACTION_HEADSET_PLUG (wired headset)
     * - BluetoothManager for Bluetooth device detection
     */
    private fun registerLegacyListeners() {
        // Register BroadcastReceiver for wired headset plug/unplug
        headsetPlugReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    val hasMicrophone = intent.getIntExtra("microphone", -1) == 1
                    logger.d {
                        "[onReceive] ACTION_HEADSET_PLUG: state=$state, hasMicrophone=$hasMicrophone"
                    }
                    // Re-enumerate devices when headset is plugged/unplugged
                    onDeviceChange()
                }
            }
        }

        val filter = IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        context.registerReceiver(headsetPlugReceiver, filter)

        // Initialize Bluetooth adapter and headset profile proxy
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            logger.i { "[registerLegacyListeners] Device does not support Bluetooth" }
            return
        }

        // Check if Bluetooth SCO is available
        @Suppress("DEPRECATION")
        if (!audioManager.isBluetoothScoAvailableOffCall) {
            logger.w { "[registerLegacyListeners] Bluetooth SCO audio is not available off call" }
            return
        }

        // Get BluetoothHeadset profile proxy
        logger.d { "[registerLegacyListeners] Requesting BluetoothHeadset profile proxy" }
        val profileProxyResult = try {
            bluetoothAdapter?.getProfileProxy(
                context,
                bluetoothProfileServiceListener,
                BluetoothProfile.HEADSET,
            )
        } catch (e: SecurityException) {
            logger.w {
                "[registerLegacyListeners] SecurityException getting Bluetooth profile proxy: ${e.message}. Will fall back to AudioDeviceInfo enumeration for Bluetooth."
            }
            bluetoothProfileProxyAvailable = false
            // Continue without profile proxy - we'll use AudioDeviceInfo enumeration as fallback
            // Don't return, continue to register receivers for state changes
        }

        logger.d { "[registerLegacyListeners] getProfileProxy result: $profileProxyResult" }
        if (profileProxyResult == true) {
            bluetoothProfileProxyAvailable = true
            logger.d {
                "[registerLegacyListeners] Profile proxy requested, waiting for onServiceConnected callback"
            }
        } else {
            bluetoothProfileProxyAvailable = false
            logger.w {
                "[registerLegacyListeners] BluetoothAdapter.getProfileProxy(HEADSET) failed. Will fall back to AudioDeviceInfo enumeration for Bluetooth."
            }
        }

        // Register Bluetooth state change receiver
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when {
                    action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val connectionState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                        logger.d { "[onReceive] Bluetooth headset connection state changed: $connectionState" }
                        onHeadsetConnectionStateChanged(connectionState)
                    }
                    action == BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                        val audioState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                        logger.d { "[onReceive] Bluetooth audio state changed: $audioState" }
                        onAudioStateChanged(audioState, isInitialStickyBroadcast)
                    }
                    action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                        val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                        handleBluetoothScoStateUpdate(state)
                    }
                }
            }
        }

        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        context.registerReceiver(bluetoothStateReceiver, bluetoothFilter)
    }

    /**
     * Unregisters legacy listeners.
     */
    private fun unregisterLegacyListeners() {
        headsetPlugReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                logger.w { "[unregisterLegacyListeners] Failed to unregister headset receiver: $e" }
            }
            headsetPlugReceiver = null
        }

        bluetoothStateReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                logger.w { "[unregisterLegacyListeners] Failed to unregister Bluetooth receiver: $e" }
            }
            bluetoothStateReceiver = null
        }

        // Close BluetoothHeadset profile proxy
        bluetoothHeadset?.let { headset ->
            try {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, headset)
            } catch (e: Exception) {
                logger.w { "[unregisterLegacyListeners] Failed to close Bluetooth profile proxy: $e" }
            }
        }
        bluetoothHeadset = null
        bluetoothDevice = null

        bluetoothManager = null
        bluetoothAdapter = null
    }

    /**
     * Detects Bluetooth devices using BluetoothHeadset profile
     * This only returns devices that support SCO and are actually connected.
     */
    private fun detectBluetoothDevices(): List<StreamAudioDevice.BluetoothHeadset> {
        if (bluetoothHeadset == null) {
            logger.d { "[detectBluetoothDevices] bluetoothHeadset is null, profile proxy not connected yet" }
            return emptyList()
        }

        val devices = mutableListOf<StreamAudioDevice.BluetoothHeadset>()

        try {
            val connectedDevices: List<BluetoothDevice>? = bluetoothHeadset?.connectedDevices
            logger.d { "[detectBluetoothDevices] connectedDevices count: ${connectedDevices?.size ?: 0}" }

            if (connectedDevices.isNullOrEmpty()) {
                logger.d { "[detectBluetoothDevices] No connected Bluetooth headsets" }
                return emptyList()
            }

            // Get the first connected device
            val device = connectedDevices[0]
            val deviceName = device.name ?: "Bluetooth Headset"
            val deviceAddress = device.address ?: ""

            // Check connection state
            val connectionState = try {
                bluetoothHeadset?.getConnectionState(device)
            } catch (e: SecurityException) {
                logger.w { "[detectBluetoothDevices] SecurityException getting connection state: ${e.message}" }
                BluetoothHeadset.STATE_DISCONNECTED
            }

            logger.d {
                "[detectBluetoothDevices] Found Bluetooth device: name=$deviceName, " +
                    "address=$deviceAddress, connectionState=$connectionState"
            }

            // Only add device if it's actually connected
            if (connectionState == BluetoothHeadset.STATE_CONNECTED) {
                // Create StreamAudioDevice for the Bluetooth headset
                // Note: We don't have AudioDeviceInfo here, but that's okay - we know it supports SCO
                devices.add(
                    StreamAudioDevice.BluetoothHeadset(
                        name = deviceName,
                        audioDeviceInfo = null, // We don't have AudioDeviceInfo from headset profile
                    ),
                )

                bluetoothDevice = device
                logger.d { "[detectBluetoothDevices] Added Bluetooth device to list" }
            } else {
                logger.d {
                    "[detectBluetoothDevices] Device not in connected state, skipping. State: $connectionState"
                }
            }
        } catch (e: SecurityException) {
            logger.w { "[detectBluetoothDevices] SecurityException getting Bluetooth devices: ${e.message}" }
        } catch (e: Exception) {
            logger.e(e) { "[detectBluetoothDevices] Error detecting Bluetooth devices" }
        }

        return devices
    }

    /**
     * BluetoothProfile.ServiceListener for BluetoothHeadset profile proxy.
     */
    private val bluetoothProfileServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HEADSET) {
                mainHandler.post {
                    logger.i { "[onServiceConnected] BluetoothHeadset profile connected, proxy=$proxy" }
                    bluetoothHeadset = proxy as? BluetoothHeadset
                    bluetoothProfileProxyAvailable = true

                    // Immediately check for connected devices and trigger enumeration
                    if (bluetoothHeadset != null) {
                        logger.d { "[onServiceConnected] Profile connected, checking for devices" }
                        try {
                            val connectedDevices = bluetoothHeadset?.connectedDevices
                            logger.d { "[onServiceConnected] Connected devices count: ${connectedDevices?.size ?: 0}" }
                            if (!connectedDevices.isNullOrEmpty()) {
                                connectedDevices.forEachIndexed { index, device ->
                                    val connectionState = try {
                                        bluetoothHeadset?.getConnectionState(device)
                                    } catch (e: SecurityException) {
                                        logger.w { "[onServiceConnected] SecurityException getting connection state: ${e.message}" }
                                        -1
                                    }
                                    logger.d {
                                        "[onServiceConnected] Device $index: name=${device.name}, " +
                                            "address=${device.address}, state=$connectionState"
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            logger.w { "[onServiceConnected] SecurityException checking devices: ${e.message}" }
                        }
                    }

                    onDeviceChange()
                }
            } else {
                logger.w { "[onServiceConnected] Unexpected profile: $profile" }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                mainHandler.post {
                    logger.i { "[onServiceDisconnected] BluetoothHeadset profile disconnected" }
                    stopBluetoothSco()
                    bluetoothHeadset = null
                    bluetoothDevice = null
                    bluetoothProfileProxyAvailable = false
                    onDeviceChange()
                }
            } else {
                logger.w { "[onServiceDisconnected] Unexpected profile: $profile" }
            }
        }
    }

    /**
     * Handles Bluetooth headset connection state changes.
     */
    private fun onHeadsetConnectionStateChanged(connectionState: Int) {
        logger.d { "[onHeadsetConnectionStateChanged] Connection state: $connectionState" }
        when (connectionState) {
            BluetoothHeadset.STATE_CONNECTED -> {
                bluetoothScoConnectionAttempts = 0
                onDeviceChange()
            }
            BluetoothHeadset.STATE_DISCONNECTED -> {
                stopBluetoothSco()
                onDeviceChange()
            }
        }
    }

    /**
     * Handles Bluetooth audio state changes.
     */
    private fun onAudioStateChanged(audioState: Int, isInitialStateChange: Boolean) {
        logger.d { "[onAudioStateChanged] Audio state: $audioState, isInitial: $isInitialStateChange" }

        if (audioState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            bluetoothScoTimeoutRunnable?.let { bluetoothScoTimeoutHandler.removeCallbacks(it) }
            if (bluetoothScoState == BluetoothScoState.CONNECTING) {
                logger.d { "[onAudioStateChanged] Bluetooth audio SCO is now connected" }
                bluetoothScoState = BluetoothScoState.CONNECTED
                bluetoothScoConnectionAttempts = 0
            }
        } else if (audioState == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
            if (isInitialStateChange) {
                logger.d { "[onAudioStateChanged] Ignoring initial sticky broadcast for audio disconnect" }
                return
            }
            logger.d { "[onAudioStateChanged] Bluetooth audio SCO is now disconnected" }
            onDeviceChange()
        }
    }

    /**
     * Handles Bluetooth SCO audio state updates.
     */
    private fun handleBluetoothScoStateUpdate(state: Int) {
        logger.d { "[handleBluetoothScoStateUpdate] SCO state: $state" }
        when (state) {
            AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                if (bluetoothScoState == BluetoothScoState.CONNECTING) {
                    logger.d { "[handleBluetoothScoStateUpdate] Bluetooth SCO connected" }
                    bluetoothScoTimeoutRunnable?.let {
                        bluetoothScoTimeoutHandler.removeCallbacks(
                            it,
                        )
                    }
                    bluetoothScoState = BluetoothScoState.CONNECTED
                    bluetoothScoConnectionAttempts = 0
                } else {
                    logger.w { "[handleBluetoothScoStateUpdate] Unexpected SCO connected state: $bluetoothScoState" }
                }
            }
            AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                if (bluetoothScoState == BluetoothScoState.CONNECTED ||
                    bluetoothScoState == BluetoothScoState.CONNECTING
                ) {
                    logger.d { "[handleBluetoothScoStateUpdate] Bluetooth SCO disconnected" }
                    bluetoothScoState = BluetoothScoState.DISCONNECTED
                    // If we were trying to connect and it disconnected, it's a failure
                    if (bluetoothScoConnectionAttempts > 0) {
                        handleBluetoothConnectionFailure()
                    }
                }
            }
            AudioManager.SCO_AUDIO_STATE_CONNECTING -> {
                logger.d { "[handleBluetoothScoStateUpdate] Bluetooth SCO connecting" }
                bluetoothScoState = BluetoothScoState.CONNECTING
            }
            AudioManager.SCO_AUDIO_STATE_ERROR -> {
                logger.w { "[handleBluetoothScoStateUpdate] Bluetooth SCO error" }
                handleBluetoothConnectionFailure()
            }
        }
    }

    /**
     * Starts Bluetooth SCO connection with robust error handling and retry logic.
     * checks if SCO is available and device is connected.
     */
    private fun startBluetoothSco(): Boolean {
        if (bluetoothScoConnectionAttempts >= maxBluetoothScoAttempts) {
            logger.w { "[startBluetoothSco] SCO connection attempts maxed out" }
            return false
        }

        if (bluetoothHeadset == null || bluetoothDevice == null) {
            logger.w { "[startBluetoothSco] No Bluetooth headset or device available" }
            return false
        }

        // Check if device is actually connected via headset profile
        try {
            val connectionState = bluetoothHeadset?.getConnectionState(bluetoothDevice)
            if (connectionState != BluetoothHeadset.STATE_CONNECTED) {
                logger.w { "[startBluetoothSco] Bluetooth device not connected, state: $connectionState" }
                return false
            }
        } catch (e: SecurityException) {
            logger.w { "[startBluetoothSco] SecurityException checking connection state: ${e.message}" }
            return false
        }

        if (bluetoothScoState == BluetoothScoState.CONNECTED ||
            bluetoothScoState == BluetoothScoState.CONNECTING
        ) {
            logger.d { "[startBluetoothSco] Already connected or connecting, state: $bluetoothScoState" }
            return true
        }

        bluetoothScoState = BluetoothScoState.CONNECTING
        bluetoothScoConnectionAttempts++

        logger.d { "[startBluetoothSco] Starting Bluetooth SCO (attempt $bluetoothScoConnectionAttempts)" }

        try {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true

            // Set up timeout to detect connection failure
            // The ACTION_SCO_AUDIO_STATE_UPDATED and ACTION_AUDIO_STATE_CHANGED receivers will notify us when connection succeeds
            bluetoothScoTimeoutRunnable = Runnable {
                if (bluetoothScoState == BluetoothScoState.CONNECTING) {
                    // Check if actually connected
                    val isAudioConnected = try {
                        bluetoothHeadset?.isAudioConnected(bluetoothDevice) == true
                    } catch (e: SecurityException) {
                        false
                    }

                    if (isAudioConnected) {
                        logger.i { "[startBluetoothSco] Device actually connected, not timed out" }
                        bluetoothScoState = BluetoothScoState.CONNECTED
                        bluetoothScoConnectionAttempts = 0
                    } else {
                        logger.w { "[startBluetoothSco] Connection timeout after ${BLUETOOTH_SCO_CONNECTION_TIMEOUT_MS}ms" }
                        stopBluetoothSco()
                        handleBluetoothConnectionFailure()
                    }
                }
            }
            bluetoothScoTimeoutHandler.postDelayed(
                bluetoothScoTimeoutRunnable!!,
                BLUETOOTH_SCO_CONNECTION_TIMEOUT_MS,
            )

            logger.i { "[startBluetoothSco] SCO audio started successfully" }
            return true
        } catch (e: Exception) {
            logger.e(e) { "[startBluetoothSco] Failed to start Bluetooth SCO" }
            bluetoothScoState = BluetoothScoState.DISCONNECTED
            handleBluetoothConnectionFailure()
            return false
        }
    }

    /**
     * Stops Bluetooth SCO connection.
     */
    private fun stopBluetoothSco() {
        if (bluetoothScoState != BluetoothScoState.CONNECTING && bluetoothScoState != BluetoothScoState.CONNECTED) {
            logger.d { "[stopBluetoothSco] Skipping SCO stop due to state: $bluetoothScoState" }
            return
        }

        logger.d { "[stopBluetoothSco] Stopping Bluetooth SCO" }

        bluetoothScoTimeoutRunnable?.let {
            bluetoothScoTimeoutHandler.removeCallbacks(it)
            bluetoothScoTimeoutRunnable = null
        }

        try {
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
            bluetoothScoState = BluetoothScoState.DISCONNECTED
            bluetoothScoConnectionAttempts = 0
            logger.i { "[stopBluetoothSco] SCO audio stopped successfully" }
        } catch (e: Exception) {
            logger.e(e) { "[stopBluetoothSco] Failed to stop Bluetooth SCO" }
            // Even if stop fails, mark as disconnected
            bluetoothScoState = BluetoothScoState.DISCONNECTED
            bluetoothScoConnectionAttempts = 0
        }
    }

    /**
     * Handles Bluetooth connection failure by reverting to earpiece (matching Twilio behavior).
     */
    private fun handleBluetoothConnectionFailure() {
        logger.w { "[handleBluetoothConnectionFailure] Bluetooth connection failed, reverting to earpiece" }

        bluetoothScoState = BluetoothScoState.DISCONNECTED
        bluetoothScoTimeoutRunnable?.let {
            bluetoothScoTimeoutHandler.removeCallbacks(it)
            bluetoothScoTimeoutRunnable = null
        }

        // Reset attempts after max retries
        if (bluetoothScoConnectionAttempts >= maxBluetoothScoAttempts) {
            logger.w { "[handleBluetoothConnectionFailure] Max attempts reached, resetting counter" }
            bluetoothScoConnectionAttempts = 0
        }

        // Trigger Bluetooth connection failure callback to allow StreamAudioSwitch to handle fallback
        onBluetoothConnectionFailure()
    }

    /**
     * Checks if the device has an earpiece (i.e., is a phone).
     * checks for telephony feature.
     */
    private fun hasEarpiece(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    companion object {
        private const val TAG = "LegacyAudioDeviceManager"
        private const val BLUETOOTH_SCO_CONNECTION_TIMEOUT_MS = 5000L
    }
}
