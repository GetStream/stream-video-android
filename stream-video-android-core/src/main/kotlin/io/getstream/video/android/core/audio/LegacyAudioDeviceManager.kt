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

    /**
     * Build a deduplicated list of available audio devices on legacy Android (API 24–30).
     *
     * Detects Bluetooth headsets using the Bluetooth HEADSET profile proxy when available or falls back
     * to AudioDeviceInfo enumeration; deduplicates SCO vs A2DP devices by Bluetooth address (preferring
     * SCO), includes wired headset (with a fallback check), always includes speakerphone, and adds
     * earpiece only if the device has telephony capability.
     *
     * @return A list of detected StreamAudioDevice instances representing available audio routes. */
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

    /**
     * Selects the specified audio device and configures system audio routing accordingly.
     *
     * For speakerphone and wired headset selection this enables/disables the speakerphone as appropriate.
     * For earpiece selection this disables the speakerphone. For Bluetooth headset selection this
     * disables the speakerphone and initiates a Bluetooth SCO connection.
     *
     * @param device The audio device to select.
     * @return `true` if the selection and the corresponding system audio configuration were initiated, `false` otherwise.
     */
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

    /**
     * Resets audio routing to a neutral state by stopping Bluetooth SCO, turning off the speakerphone, and clearing the selected device.
     */
    override fun clearDevice() {
        stopBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
        selectedDevice = null
    }

    /**
 * Retrieve the currently selected audio device.
 *
 * @return The currently selected [StreamAudioDevice], or `null` if no device is selected.
 */
override fun getSelectedDevice(): StreamAudioDevice? = selectedDevice

    /**
     * Begin monitoring legacy audio device changes.
     *
     * Registers the legacy listeners required to detect wired headset plug events,
     * Bluetooth headset/profile state and SCO/audio state updates so device
     * availability and connection changes are reported to the manager's callbacks.
     */
    override fun start() {
        registerLegacyListeners()
    }

    /**
     * Stops the legacy audio device manager and releases audio-related resources.
     *
     * Unregisters legacy listeners, stops any ongoing Bluetooth SCO connection, and clears the currently selected device.
     */
    override fun stop() {
        unregisterLegacyListeners()
        stopBluetoothSco()
        clearDevice()
    }

    /**
     * Register legacy audio listeners and initialize Bluetooth HEADSET profile proxy for API 24–30.
     *
     * Registers a BroadcastReceiver for wired headset plug/unplug events and a BroadcastReceiver for
     * Bluetooth headset connection, audio, and SCO state updates. Initializes BluetoothManager and
     * BluetoothAdapter and requests a HEADSET profile proxy; if the device does not support Bluetooth,
     * SCO is unavailable, or a SecurityException occurs while requesting the profile proxy, the manager
     * records that the profile proxy is unavailable and continues using the fallback enumeration path.
     */
    private fun registerLegacyListeners() {
        // Register BroadcastReceiver for wired headset plug/unplug
        headsetPlugReceiver = object : BroadcastReceiver() {
            /**
             * Receives headset plug/unplug broadcasts and triggers device re-enumeration.
             *
             * @param context The context in which the receiver is running.
             * @param intent The broadcast intent; expects AudioManager.ACTION_HEADSET_PLUG and reads the `state` and `microphone` extras.
             */
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
            /**
             * Handles incoming broadcast intents related to Bluetooth headset connection, Bluetooth audio state, and SCO audio state,
             * extracting the relevant state values and dispatching them to the manager's internal handlers.
             *
             * @param context The Context in which the receiver is running.
             * @param intent The received Intent; expected to contain one of:
             *  - BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED (contains BluetoothHeadset.EXTRA_STATE)
             *  - BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED (contains BluetoothHeadset.EXTRA_STATE)
             *  - AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED (contains AudioManager.EXTRA_SCO_AUDIO_STATE)
             */
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
     * Stops and cleans up legacy audio listeners and Bluetooth resources.
     *
     * Unregisters the wired-headset and Bluetooth broadcast receivers if registered,
     * closes the Bluetooth HEADSET profile proxy when available, and clears related
     * Bluetooth manager, adapter, headset proxy, and device references.
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
     * Detects connected Bluetooth headsets that support SCO.
     *
     * Returns a list of BluetoothHeadset devices that are currently connected via the HEADSET profile and suitable for SCO audio.
     *
     * @return A list of StreamAudioDevice.BluetoothHeadset representing connected SCO-capable Bluetooth headsets; empty if none are available or the HEADSET profile proxy is not connected.
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
        /**
         * Handles Bluetooth profile connection events for the HEADSET profile.
         *
         * When the HEADSET profile is connected, stores the provided proxy as the internal
         * `BluetoothHeadset` proxy, marks the profile proxy as available, inspects any
         * currently connected Bluetooth devices (logging their name, address, and connection state),
         * and triggers device enumeration via `onDeviceChange()`. SecurityExceptions thrown
         * while querying device state are caught and logged. Logs a warning for unexpected profiles.
         *
         * @param profile The Bluetooth profile constant (from `BluetoothProfile`) that was connected.
         * @param proxy The profile proxy instance; expected to be castable to `BluetoothHeadset` when `profile`
         *              equals `BluetoothProfile.HEADSET`. May be null.
         */
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

        /**
         * Handles disconnection of a Bluetooth profile service and updates internal Bluetooth state.
         *
         * When the HEADSET profile is disconnected, posts a task to the main handler that stops
         * Bluetooth SCO, clears the HEADSET profile proxy and targeted Bluetooth device, marks
         * the profile proxy as unavailable, and triggers `onDeviceChange()`. For other profiles,
         * logs a warning indicating an unexpected profile disconnection.
         */
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
     * Update internal state and notify listeners when the Bluetooth HEADSET profile connection state changes.
     *
     * For `BluetoothHeadset.STATE_CONNECTED` this resets SCO connection attempts and signals device availability changes.
     * For `BluetoothHeadset.STATE_DISCONNECTED` this stops any active Bluetooth SCO connection and signals device availability changes.
     *
     * @param connectionState The Bluetooth HEADSET profile connection state (one of `BluetoothHeadset.STATE_CONNECTED`,
     * `BluetoothHeadset.STATE_DISCONNECTED`, etc.).
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
     * Process Bluetooth headset audio connection state updates and adjust internal SCO state.
     *
     * Cancels any pending SCO connection timeout when audio becomes connected; if the manager
     * was attempting to connect, marks SCO as connected and resets retry attempts. When audio
     * becomes disconnected, ignores the initial sticky broadcast and otherwise triggers a device
     * availability update.
     *
     * @param audioState One of the BluetoothHeadset audio state constants (e.g. `STATE_AUDIO_CONNECTED`, `STATE_AUDIO_DISCONNECTED`).
     * @param isInitialStateChange `true` when the received broadcast represents an initial sticky state delivered on registration, otherwise `false`.
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
     * Updates internal Bluetooth SCO state and reacts to SCO audio state changes.
     *
     * Updates the manager's internal SCO state to CONNECTING, CONNECTED, or DISCONNECTED based on the provided
     * AudioManager SCO state, clears any pending SCO connection timeout when connected, resets connection attempts
     * after a successful connection, and invokes Bluetooth connection failure handling on errors or failed connect attempts.
     *
     * @param state One of the AudioManager.SCO_AUDIO_STATE_* constants indicating the current SCO audio state.
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
     * Initiates a Bluetooth SCO audio connection with attempt limits and failure handling.
     *
     * Verifies that a headset profile proxy and target device are available and connected, enforces
     * the maximum retry limit, updates internal SCO state and attempt counters, and schedules a
     * timeout to detect and handle connection failures.
     *
     * @return `true` if the SCO start was initiated or SCO is already connecting/connected, `false` otherwise.
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
     * Stops an active or pending Bluetooth SCO connection.
     *
     * Takes no action if SCO is not currently connecting or connected.
     * Resets the connection attempt counter for future reconnection attempts.
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
     * Determines whether the device includes a built-in earpiece (has telephony capability).
     *
     * @return `true` if the device reports the telephony feature, `false` otherwise.
     */
    private fun hasEarpiece(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    companion object {
        private const val TAG = "LegacyAudioDeviceManager"
        private const val BLUETOOTH_SCO_CONNECTION_TIMEOUT_MS = 5000L
    }
}