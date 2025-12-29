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

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioRecord.READ_BLOCKING
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.getstream.android.video.generated.models.VideoSettingsResponse
import io.getstream.log.taggedLogger
import io.getstream.result.extractCause
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioHandlerFactory
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.audio.StreamAudioDevice.Companion.fromAudio
import io.getstream.video.android.core.call.video.FilterVideoProcessor
import io.getstream.video.android.core.camera.CameraCharacteristicsValidator
import io.getstream.video.android.core.camera.DefaultCameraCharacteristicsValidator
import io.getstream.video.android.core.screenshare.StreamScreenShareService
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaStreamTrack
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import stream.video.sfu.models.AudioBitrateProfile
import stream.video.sfu.models.VideoDimension
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.resumeWithException

sealed class DeviceStatus {
    data object NotSelected : DeviceStatus()
    data object Disabled : DeviceStatus()
    data object Enabled : DeviceStatus()
}

data class CameraDeviceWrapped(
    val id: String,
    val characteristics: CameraCharacteristics?,
    val supportedFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>?,
    val maxResolution: Int,
    val direction: CameraDirection?,
)

class SpeakerManager(
    val mediaManager: MediaManagerImpl,
    val microphoneManager: MicrophoneManager,
    val initialVolume: Int? = null,
    val audioUsageProvider: (() -> Int) = { defaultAudioUsage },
) {

    private val logger by taggedLogger("Media:SpeakerManager")

    private var priorVolume: Int? = null
    private val _volume = MutableStateFlow(initialVolume)
    val volume: StateFlow<Int?> = _volume

    /** The status of the audio */
    internal val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    /** Represents whether the speakerphone is enabled */
    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    /** The current audio usage value (USAGE_MEDIA or USAGE_VOICE_COMMUNICATION) */
    private val _audioUsage = MutableStateFlow(audioUsageProvider.invoke())
    val audioUsage: StateFlow<Int> = _audioUsage

    val selectedDevice: StateFlow<StreamAudioDevice?> = microphoneManager.selectedDevice

    val devices: StateFlow<List<StreamAudioDevice>> = microphoneManager.devices

    private val _speakerPhoneEnabled = MutableStateFlow(true)
    val speakerPhoneEnabled: StateFlow<Boolean> = _speakerPhoneEnabled

    internal var selectedBeforeSpeaker: StreamAudioDevice? = null

    internal fun enable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Enabled
        }
        setSpeakerPhone(true)
    }

    fun disable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Disabled
        }
        setSpeakerPhone(false)
    }

    /**
     * Enable or disable the speakerphone.
     */
    fun setEnabled(enabled: Boolean, fromUser: Boolean = true) {
        logger.i { "setEnabled $enabled" }
        // TODO: what is fromUser?
        if (enabled) {
            enable(fromUser = fromUser)
        } else {
            disable(fromUser = fromUser)
        }
    }

    /**
     * Enables or disables the speakerphone.
     *
     * When the speaker is disabled the device that gets selected next is by default the first device
     * that is NOT a speakerphone. To override this use [defaultFallback].
     * If you want the earpice to be selected if the speakerphone is disabled do
     * ```kotlin
     * setSpeakerPhone(enable, StreamAudioDevice.Earpiece)
     * ```
     *
     * @param enable if true, enables the speakerphone, if false disables it and selects another device.
     * @param defaultFallback when [enable] is false this is used to select the next device after the speaker.
     * */
    fun setSpeakerPhone(enable: Boolean, defaultFallback: StreamAudioDevice? = null) {
        microphoneManager.enforceSetup(preferSpeaker = enable) {
            val devices = devices.value
            if (enable) {
                val speaker =
                    devices.filterIsInstance<StreamAudioDevice.Speakerphone>().firstOrNull()
                selectedBeforeSpeaker = selectedDevice.value.takeUnless {
                    it is StreamAudioDevice.Speakerphone
                } ?: devices.firstOrNull {
                    it !is StreamAudioDevice.Speakerphone
                }

                logger.d { "#deviceDebug; selectedBeforeSpeaker: $selectedBeforeSpeaker" }

                _speakerPhoneEnabled.value = true
                microphoneManager.select(speaker)
            } else {
                _speakerPhoneEnabled.value = false
                // swap back to the old one
                val defaultFallbackFromType = defaultFallback?.let {
                    devices.filterIsInstance(defaultFallback::class.java)
                }?.firstOrNull()

                val firstNonSpeaker = devices.firstOrNull { it !is StreamAudioDevice.Speakerphone }

                val fallback: StreamAudioDevice? = when {
                    defaultFallbackFromType != null -> defaultFallbackFromType
                    selectedBeforeSpeaker != null &&
                        selectedBeforeSpeaker !is StreamAudioDevice.Speakerphone &&
                        devices.contains(selectedBeforeSpeaker) -> selectedBeforeSpeaker

                    else -> firstNonSpeaker
                }

                microphoneManager.select(fallback)
            }
        }
    }

    /**
     * Set the volume as a percentage, 0-100
     */
    fun setVolume(volumePercentage: Int) {
        microphoneManager.enforceSetup {
            microphoneManager.audioManager?.let {
                val max = it.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val level = max / 100 * volumePercentage
                _volume.value = volumePercentage
                it.setStreamVolume(AudioManager.STREAM_VOICE_CALL, level, 0)
            }
        }
    }

    fun pause() {
        priorVolume = _volume.value
        setVolume(0)
    }

    fun resume() {
        priorVolume?.let {
            if (it > 0) {
                setVolume(it)
            }
        }
    }

    /**
     * Set the audio usage value.
     * This allows toggling between USAGE_MEDIA and USAGE_VOICE_COMMUNICATION.
     * Updates the StateFlow immediately for responsive UI, then attempts to update the ADM.
     * If the ADM update fails, the StateFlow is rolled back to the previous value.
     *
     * @param audioUsage The audio usage value to set (e.g., AudioAttributes.USAGE_MEDIA or AudioAttributes.USAGE_VOICE_COMMUNICATION)
     * @return true if the update was successful, false otherwise
     */
    fun setAudioUsage(audioUsage: Int): Boolean {
        logger.i { "setAudioUsage: $audioUsage" }

        val success = mediaManager.call.peerConnectionFactory.updateAudioTrackUsage(audioUsage)
        if (success) {
            _audioUsage.value = audioUsage
            logger.d { "setAudioUsage: successfully updated audio usage to $audioUsage" }
            return true
        } else {
            // Rollback to previous value if ADM update failed
            logger.w { "setAudioUsage: failed to update audio usage to $audioUsage" }
            return false
        }
    }
}

class ScreenShareManager(
    val mediaManager: MediaManagerImpl,
    val eglBaseContext: EglBase.Context,
) {

    companion object {
        // TODO: This could be configurable by the client
        internal val screenShareResolution = VideoDimension(1920, 1080)
        internal val screenShareBitrate = 1_000_000
        internal val screenShareFps = 15
        private const val INPUT_NUM_OF_CHANNELS = 1 // 1 for mono, 2 for stereo output

        // Requested size of each recorded buffer provided to the client.
        private const val CALLBACK_BUFFER_SIZE_MS = 10

        // Average number of callbacks per second.
        private const val BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS

        // Bits per sample (16-bit PCM)
        private const val INPUT_BITS_PER_SAMPLE = 16
    }

    private val logger by taggedLogger("Media:ScreenShareManager")

    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    private val _audioEnabled = MutableStateFlow<Boolean>(false)

    /** Represents whether screen share audio is enabled */
    public val audioEnabled: StateFlow<Boolean> = _audioEnabled

    private lateinit var screenCapturerAndroid: ScreenCapturerAndroid
    internal lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private var setupCompleted = false
    private var isScreenSharing = false
    private var mediaProjectionPermissionResultData: Intent? = null
    private var mediaProjection: MediaProjection? = null
    private var screenAudioRecord: AudioRecord? = null
    private val inputSampleRate = 48000 // Standard WebRTC sample rate

    // ByteBuffer for reading screen audio on demand
    private var screenAudioBuffer: ByteBuffer? = null

    /**
     * The [ServiceConnection.onServiceConnected] is called when our [StreamScreenShareService]
     * has started. At this point we can start screen-sharing. Starting the screen-sharing without
     * waiting for the Service to start would throw an exception (
     */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (isScreenSharing) {
                logger.w { "We are already screen-sharing - ignoring call to start another screenshare" }
                return
            }

            // Create the ScreenCapturerAndroid from webrtc-android
            screenCapturerAndroid =
                ScreenCapturerAndroid(
                    mediaProjectionPermissionResultData,
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            super.onStop()
                            // User can also disable screen sharing from the system menu
                            disable()
                        }
                    },
                )

            // initialize it
            screenCapturerAndroid.initialize(
                surfaceTextureHelper,
                mediaManager.context,
                mediaManager.screenShareVideoSource.capturerObserver,
            )

            // start
            screenCapturerAndroid.startCapture(
                screenShareResolution.width,
                screenShareResolution.height,
                0,
            )

            // Get MediaProjection from ScreenCapturerAndroid
            mediaProjection = screenCapturerAndroid.mediaProjection

            // Start screen audio capture only if audio is enabled
            if (_audioEnabled.value) {
                startScreenAudioCapture()
            }

            isScreenSharing = true
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    fun enable(mediaProjectionPermissionResultData: Intent, fromUser: Boolean = true, includeAudio: Boolean = false) {
        mediaManager.screenShareTrack.setEnabled(true)
        if (fromUser) {
            _status.value = DeviceStatus.Enabled
        }
        _audioEnabled.value = includeAudio
        setup()
        startScreenShare(mediaProjectionPermissionResultData)
    }

    fun disable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Disabled
        }
        _audioEnabled.value = false

        if (isScreenSharing) {
            mediaManager.screenShareTrack.setEnabled(false)
            screenCapturerAndroid.stopCapture()
            stopScreenAudioCapture()
            mediaManager.context.stopService(
                Intent(mediaManager.context, StreamScreenShareService::class.java),
            )
            isScreenSharing = false
        }
    }

    /**
     * Gets the next set of screen audio bytes on demand by reading directly from AudioRecord.
     * Returns null if screen audio capture is not active.
     * This method is called from the AudioBufferCallback in StreamPeerConnectionFactory when mixing is needed.
     *
     * @param bytesRequested The number of bytes requested
     * @return ByteBuffer containing the requested bytes (may have fewer bytes if not enough data is available), or null if no data
     */
    internal fun getScreenAudioBytes(bytesRequested: Int): ByteBuffer? {
        val record = screenAudioRecord ?: return null

        if (bytesRequested <= 0) return null

        // Ensure buffer has enough capacity
        val buffer = screenAudioBuffer?.takeIf { it.capacity() >= bytesRequested }
            ?: ByteBuffer.allocateDirect(bytesRequested).also { screenAudioBuffer = it }

        buffer.clear()
        buffer.limit(bytesRequested)

        // Read directly from AudioRecord using READ_BLOCKING mode
        val bytesRead = record.read(buffer, bytesRequested, READ_BLOCKING)

        if (bytesRead > 0) {
            buffer.limit(bytesRead)
            // Return a duplicate to avoid position/limit conflicts with concurrent access
            return buffer
        }

        return null
    }

    /**
     * Starts capturing screen audio using AudioRecord with AudioPlaybackCaptureConfiguration.
     */
    private fun startScreenAudioCapture() {
        val mediaProj = mediaProjection ?: run {
            logger.e { "MediaProjection is null, cannot start screen audio capture" }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            logger.w { "Screen audio capture requires Android Q (API 29) or higher" }
            return
        }

        if (ActivityCompat.checkSelfPermission(
                mediaManager.context,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.w { "RECORD_AUDIO permission not granted, cannot capture screen audio" }
            return
        }

        try {
            // Calculate buffer size using the correct formula
            val bytesPerFrame: Int = INPUT_NUM_OF_CHANNELS * (INPUT_BITS_PER_SAMPLE / 8)
            val capacity = bytesPerFrame * (inputSampleRate / BUFFERS_PER_SECOND)

            // Create ByteBuffer for reading audio on demand
            screenAudioBuffer = ByteBuffer.allocateDirect(capacity)

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(inputSampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProj)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            screenAudioRecord = AudioRecord.Builder()
                .setAudioFormat(format)
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .build()

            screenAudioRecord?.startRecording()

            logger.d { "Screen audio capture started" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to start screen audio capture" }
        }
    }

    /**
     * Stops capturing screen audio and releases resources.
     */
    private fun stopScreenAudioCapture() {
        try {
            screenAudioRecord?.stop()
            screenAudioRecord?.release()
            screenAudioRecord = null
            logger.d { "Screen audio capture stopped" }
        } catch (e: Exception) {
            logger.e(e) { "Error stopping screen audio capture" }
        }

        // Note: MediaProjection is managed by ScreenCapturerAndroid and will be stopped
        // when screenCapturerAndroid.stopCapture() is called, so we don't need to stop it here
        mediaProjection = null
        screenAudioBuffer = null
    }

    private fun startScreenShare(mediaProjectionPermissionResultData: Intent) {
        mediaManager.scope.launch {
            this@ScreenShareManager.mediaProjectionPermissionResultData =
                mediaProjectionPermissionResultData

            // Screen sharing requires a foreground service with foregroundServiceType "mediaProjection" to be started first.
            // We can wait for the service to be ready by binding to it and then starting the
            // media projection in onServiceConnected.
            val intent = StreamScreenShareService.createIntent(
                mediaManager.context,
                mediaManager.call.cid,
            )
            ContextCompat.startForegroundService(
                mediaManager.context,
                StreamScreenShareService.createIntent(mediaManager.context, mediaManager.call.cid),
            )
            mediaManager.context.bindService(intent, connection, 0)
        }
    }

    private fun setup() {
        if (setupCompleted) {
            return
        }

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)

        setupCompleted = true
    }
}

/**
 * The Microphone manager makes it easy to use your microphone in a call
 *
 * @sample
 *
 * val call = client.call("default", "123")
 * val microphone = call.microphone
 *
 * microphone.enable() // enable the microphone
 * microphone.disable() // disable the microphone
 * microphone.setEnabled(true) // enable the microphone
 * microphone.setSpeaker(true) // enable the speaker
 *
 * microphone.listDevices() // return stateflow with the list of available devices
 * microphone.status // the status of the microphone
 * microphone.selectedDevice // the selected device
 * microphone.speakerPhoneEnabled // the status of the speaker. true/false
 */
class MicrophoneManager(
    val mediaManager: MediaManagerImpl,
    @Deprecated("Use audioUsageProvider instead", replaceWith = ReplaceWith("audioUsageProvider"))
    val audioUsage: Int,
    val audioUsageProvider: (() -> Int),
) {
    // Internal data
    private val logger by taggedLogger("Media:MicrophoneManager")

    private lateinit var audioHandler: AudioHandler
    private var setupCompleted: Boolean = false
    internal var audioManager: AudioManager? = null
    internal var priorStatus: DeviceStatus? = null

    // Exposed state
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)

    /** The status of the audio */
    val status: StateFlow<DeviceStatus> = _status

    /** Represents whether the audio is enabled */
    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    private val _selectedDevice = MutableStateFlow<StreamAudioDevice?>(null)
    internal var nonHeadsetFallbackDevice: StreamAudioDevice? = null

    /** Currently selected device */
    val selectedDevice: StateFlow<StreamAudioDevice?> = _selectedDevice

    private val _devices = MutableStateFlow<List<StreamAudioDevice>>(emptyList())

    /** List of available devices. */
    val devices: StateFlow<List<StreamAudioDevice>> = _devices

    private val _audioBitrateProfile =
        MutableStateFlow<AudioBitrateProfile>(
            AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED,
        )

    /** The current audio bitrate profile */
    val audioBitrateProfile: StateFlow<AudioBitrateProfile> = _audioBitrateProfile

    // API
    /** Enable the audio, the rtc engine will automatically inform the SFU */
    internal fun enable(fromUser: Boolean = true) {
        enforceSetup {
            if (fromUser) {
                _status.value = DeviceStatus.Enabled
            }
            mediaManager.audioTrack.trySetEnabled(true)
        }
    }

    fun pause(fromUser: Boolean = true) {
        enforceSetup {
            // pause the microphone, and when resuming switched back to the previous state
            priorStatus = _status.value
            disable(fromUser = fromUser)
        }
    }

    fun resume(fromUser: Boolean = true) {
        enforceSetup {
            priorStatus?.let {
                if (it == DeviceStatus.Enabled) {
                    enable(fromUser = fromUser)
                }
            }
        }
    }

    /** Disable the audio track. Audio is still captured, but not send.
     * This allows for the "you are muted" toast to indicate you are talking while muted */
    fun disable(fromUser: Boolean = true) {
        enforceSetup {
            if (fromUser) {
                _status.value = DeviceStatus.Disabled
            }
            mediaManager.audioTrack.trySetEnabled(false)
        }
    }

    /**
     * Enable or disable the microphone
     */
    fun setEnabled(enabled: Boolean, fromUser: Boolean = true) {
        enforceSetup {
            if (enabled) {
                enable(fromUser = fromUser)
            } else {
                disable(fromUser = fromUser)
            }
        }
    }

    /**
     * Selects the given audio output device and updates related internal state.
     *
     * Requests the initialized audio handler to switch to `device`, sets the manager's selected device state, updates the speaker manager status when the speakerphone is (de)selected, and records a non-headset fallback device for future use.
     *
     * @param device The desired `StreamAudioDevice` to select, or `null` to clear selection.
     */
    fun select(device: StreamAudioDevice?) {
        logger.i { "selecting device $device" }
        ifAudioHandlerInitialized { it.selectDevice(device) }
        _selectedDevice.value = device

        if (device !is StreamAudioDevice.Speakerphone && mediaManager.speaker.isEnabled.value == true) {
            mediaManager.speaker._status.value = DeviceStatus.Disabled
        }

        if (device is StreamAudioDevice.Speakerphone) {
            mediaManager.speaker._status.value = DeviceStatus.Enabled
        }

        if (device !is StreamAudioDevice.BluetoothHeadset && device !is StreamAudioDevice.WiredHeadset) {
            nonHeadsetFallbackDevice = device
        }
    }

    /**
     * List the devices, returns a stateflow with audio devices
     */
    fun listDevices(): StateFlow<List<StreamAudioDevice>> {
        setup()
        return devices
    }

    /**
     * Set the audio bitrate profile.
     * This can only be set before joining the call. Once the call is joined,
     * changes to the audio bitrate profile will be ignored.
     *
     * @param profile The audio bitrate profile to use
     * @return true if the profile was successfully set, false if:
     *   - The call is already joined and the change was ignored
     *   - HiFi audio is not enabled in dashboard settings when trying to use MUSIC_HIGH_QUALITY
     */
    suspend fun setAudioBitrateProfile(profile: AudioBitrateProfile): Result<Unit> {
        val connectionState = mediaManager.call.state.connection.value
        val isJoined = connectionState is RealtimeConnection.Joined || connectionState is RealtimeConnection.Connected

        // get the call settings if they are not present
        if (mediaManager.call.state.settings.value == null) {
            val result = mediaManager.call.get()
            if (result.isFailure) {
                logger.w {
                    "[setAudioBitrateProfile] call.get() returned error: ${result.errorOrNull()?.extractCause()}"
                }
                return Result.failure(IllegalStateException(result.errorOrNull()?.extractCause()))
            }
        }

        // Check if HiFi audio is enabled in dashboard
        val hifiAudioEnabled = mediaManager.call.state.settings.value?.audio?.hifiAudioEnabled ?: false
        if (!hifiAudioEnabled) {
            logger.w {
                "[setAudioBitrateProfile] called but HiFi audio is not enabled " +
                    "in dashboard settings. ."
            }
            return Result.failure(
                IllegalArgumentException("Hi-fi audio is not enabled on dashboard settings"),
            )
        }

        if (isJoined) {
            logger.w {
                "[setAudioBitrateProfile] called after call is joined. " +
                    "Audio bitrate profile can only be set before joining the call. " +
                    "Ignoring the change."
            }
            return Result.failure(
                IllegalStateException(
                    "Cannot call setAudioBitrateProfile after call has been joined",
                ),
            )
        }

        logger.i { "[setAudioBitrateProfile] Setting audio bitrate profile to: $profile" }
        _audioBitrateProfile.value = profile
        return Result.success(Unit)
    }

    fun cleanup() {
        ifAudioHandlerInitialized { it.stop() }
        setupCompleted = false
    }

    fun canHandleDeviceSwitch() = audioUsageProvider.invoke() != AudioAttributes.USAGE_MEDIA

    /**
     * Initializes audio routing and device handling for the microphone manager.
     *
     * Sets up the Android AudioManager and, if device switching is supported and not already initialized,
     * creates and starts an AudioHandler configured with a device priority that respects `preferSpeaker`.
     * Once device information becomes available (or immediately if already set up), the optional
     * `onAudioDevicesUpdate` callback is invoked and internal device/selection state is updated.
     *
     * @param preferSpeaker When true, prioritizes the speakerphone over the earpiece when building the preferred device list.
     * @param onAudioDevicesUpdate Optional callback invoked once the available audio devices and current selection have been populated (or immediately if setup was already completed).
     */
    internal fun setup(preferSpeaker: Boolean = false, onAudioDevicesUpdate: (() -> Unit)? = null) {
        synchronized(this) {
            var capturedOnAudioDevicesUpdate = onAudioDevicesUpdate

            if (setupCompleted) {
                capturedOnAudioDevicesUpdate?.invoke()
                capturedOnAudioDevicesUpdate = null

                return
            }

            audioManager = mediaManager.context.getSystemService()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                audioManager?.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
            }

            if (canHandleDeviceSwitch() && !::audioHandler.isInitialized) {
                // Use default priority (Bluetooth -> Wired -> Earpiece -> Speakerphone)
                // unless preferSpeaker is true, then prioritize speakerphone over earpiece
                val preferredDeviceList = listOf(
                    StreamAudioDevice.BluetoothHeadset::class.java,
                    StreamAudioDevice.WiredHeadset::class.java,
                ) + if (preferSpeaker) {
                    listOf(
                        StreamAudioDevice.Speakerphone::class.java,
                        StreamAudioDevice.Earpiece::class.java,
                    )
                } else {
                    listOf(
                        StreamAudioDevice.Earpiece::class.java,
                        StreamAudioDevice.Speakerphone::class.java,
                    )
                }

                audioHandler = AudioHandlerFactory.create(
                    context = mediaManager.context,
                    preferredStreamAudioDeviceList = preferredDeviceList,
                    twilioAudioDeviceChangeListener = { devices, selected ->
                        logger.i { "[audioSwitch] audio devices. selected $selected, available devices are $devices" }

                        _devices.value = devices.map { it.fromAudio() }
                        _selectedDevice.value = selected?.fromAudio()

                        setupCompleted = true

                        capturedOnAudioDevicesUpdate?.invoke()
                        capturedOnAudioDevicesUpdate = null
                    },
                    streamAudioDeviceChangeListener = { devices, selected ->
                        logger.i { "[audioSwitch] audio devices. selected $selected, available devices are $devices" }
                        _devices.value = devices
                        _selectedDevice.value = selected

                        setupCompleted = true

                        capturedOnAudioDevicesUpdate?.invoke()
                        capturedOnAudioDevicesUpdate = null
                    },
                    useInBuiltAudioSwitch = mediaManager.useInBuiltAudioSwitch,
                )

                logger.d { "[setup] Calling start on instance $audioHandler" }
                audioHandler.start()
            } else {
                logger.d { "[MediaManager#setup] Usage is MEDIA or audioHandle is already initialized" }
                capturedOnAudioDevicesUpdate?.invoke()
            }
        }
    }

    /**
     * Ensures audio handler setup (initializing it if necessary) and runs the provided action when audio devices update.
     *
     * @param preferSpeaker If true, prefer speakerphone in the initial device priority when setting up.
     * @param actual Action to invoke when the audio devices are updated.
     */
    internal fun enforceSetup(preferSpeaker: Boolean = false, actual: () -> Unit) = setup(
        preferSpeaker,
        onAudioDevicesUpdate = actual,
    )

    /**
     * Invokes the provided block with the initialized `AudioHandler` if available.
     *
     * If the audio handler has been initialized, the `then` lambda is called with it;
     * otherwise the function logs an error indicating setup() must be called first.
     *
     * @param then Lambda to execute with the initialized `AudioHandler`.
     */
    private fun ifAudioHandlerInitialized(then: (audioHandler: AudioHandler) -> Unit) {
        if (this::audioHandler.isInitialized) {
            then(this.audioHandler)
        } else {
            logger.e { "Audio handler not initialized. Ensure calling setup(), before using the handler." }
        }
    }
}

public sealed class CameraDirection {
    public data object Front : CameraDirection()
    public data object Back : CameraDirection()
}

/**
 * The CameraManager class makes it easy to manage the camera for your video call
 *
 * @sample
 *
 * camera.enable() // enables the camera. starts capture
 * camera.disable() // disables the camera. stops capture
 * camera.flip() // flips the camera
 * camera.listDevices() // the list of available camera devices
 *
 * // stateflow objects:
 * camera.status // the status of the camera. enabled or disabled
 * camera.direction // if we're using the front facing or back facing camera
 * camera.selectedDevice // the selected camera device
 * camera.resolution // the selected camera resolution
 *
 */
class CameraManager(
    public val mediaManager: MediaManagerImpl,
    public val eglBaseContext: EglBase.Context,
    public val cameraCharacteristicsValidator: CameraCharacteristicsValidator,
    defaultCameraDirection: CameraDirection = CameraDirection.Front,
) {
    private var priorStatus: DeviceStatus? = null
    internal lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private lateinit var devices: List<CameraDeviceWrapped>
    private var isCapturingVideo: Boolean = false
    private lateinit var videoCapturer: Camera2Capturer
    private lateinit var enumerator: Camera2Enumerator
    private var cameraManager: CameraManager? = null
    private val logger by taggedLogger("Media:CameraManager")

    /** The status of the camera. enabled or disabled */
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    public val status: StateFlow<DeviceStatus> = _status

    /** Represents whether the camera is enabled */
    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    /** if we're using the front facing or back facing camera */
    private val _direction = MutableStateFlow(defaultCameraDirection)
    public val direction: StateFlow<CameraDirection> = _direction

    private val _selectedDevice = MutableStateFlow<CameraDeviceWrapped?>(null)
    public val selectedDevice: StateFlow<CameraDeviceWrapped?> = _selectedDevice

    private val _resolution = MutableStateFlow<CameraEnumerationAndroid.CaptureFormat?>(null)
    public val resolution: StateFlow<CameraEnumerationAndroid.CaptureFormat?> = _resolution

    private val _availableResolutions:
        MutableStateFlow<List<CameraEnumerationAndroid.CaptureFormat>> =
        MutableStateFlow(emptyList())
    public val availableResolutions: StateFlow<List<CameraEnumerationAndroid.CaptureFormat>> =
        _availableResolutions

    public fun listDevices(): List<CameraDeviceWrapped> {
        setup()
        return devices
    }

    internal fun enable(fromUser: Boolean = true) {
        setup()
        // 1. update our local state
        // 2. update the track enabled status
        // 3. Rtc listens and sends the update mute state request
        if (fromUser) {
            _status.value = DeviceStatus.Enabled
        }
        mediaManager.videoTrack.trySetEnabled(true)
        startCapture()
    }

    fun pause(fromUser: Boolean = true) {
        // pause the camera, and when resuming switched back to the previous state
        priorStatus = _status.value
        disable(fromUser = fromUser)
    }

    fun resume(fromUser: Boolean = true) {
        priorStatus?.let {
            if (it == DeviceStatus.Enabled) {
                enable(fromUser = fromUser)
            }
        }
    }

    fun setEnabled(enabled: Boolean, fromUser: Boolean = true) {
        if (enabled) {
            enable(fromUser = fromUser)
        } else {
            disable(fromUser = fromUser)
        }
    }

    fun disable(fromUser: Boolean = true) {
        if (fromUser) _status.value = DeviceStatus.Disabled

        if (isCapturingVideo) {
            // 1. update our local state
            // 2. update the track enabled status
            // 3. Rtc listens and sends the update mute state request
            mediaManager.videoTrack.trySetEnabled(false)
            videoCapturer.stopCapture()
            isCapturingVideo = false
        }
    }

    fun setDirection(cameraDirection: CameraDirection) {
        val previousDirection = _direction.value
        _direction.value = cameraDirection

        // flip camera if necessary
        if (previousDirection != cameraDirection) {
            flip()
        }
    }

    private var job: Job? = null

    private suspend fun flipInternal(): Boolean =
        suspendCancellableCoroutine { cont ->
            videoCapturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    if (cont.isActive) cont.resumeWith(Result.success(isFrontCamera))
                }

                override fun onCameraSwitchError(error: String?) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            RuntimeException(
                                error ?: "Unknown",
                            ),
                        )
                    }
                }
            })
            cont.invokeOnCancellation {
                // No direct cancel for switchCamera; nothing to do here.
            }
        }

    /** Flips the camera */
    fun flip() {
        logger.v { "[flip] no args" }
        if (!isCapturingVideo) {
            return
        }

        if (job != null) {
            logger.v { "[flip] job already running" }
            return
        }

        job = mediaManager.scope.launch {
            logger.v { "[flip] launched" }
            try {
                val isFront = flipInternal()
                logger.v { "[flip] isFront: $isFront" }

                // 3) Update state ONLY after success; no select()/restart.
                val newDir = if (isFront) CameraDirection.Front else CameraDirection.Back
                _direction.value = newDir

                val dev = devices.firstOrNull { it.direction == newDir }
                _selectedDevice.value = dev
                _availableResolutions.value = dev?.supportedFormats?.toList().orEmpty()

                _resolution.value = selectDesiredResolution(
                    dev?.supportedFormats,
                    mediaManager.call.state.settings.value?.video,
                )
                if (_resolution.value != null) {
                    videoCapturer.changeCaptureFormat(
                        _resolution.value!!.width,
                        _resolution.value!!.height,
                        _resolution.value!!.framerate.max,
                    )
                }

                // preview.mirror = (newDir == CameraDirection.Front)
            } catch (t: Throwable) {
                logger.e(t) { "[flip] failed" }
            } finally {
                logger.v { "[flip] finally, wait to settle" }
                delay(500)
                job = null
            }
        }
    }

    fun select(deviceId: String, triggeredByFlip: Boolean = false) {
        if (!triggeredByFlip) {
            stopCapture()
            if (!::devices.isInitialized) initDeviceList()
        }

        val selectedDevice = devices.firstOrNull { it.id == deviceId }
        if (selectedDevice != null) {
            _direction.value = selectedDevice.direction ?: CameraDirection.Back
            _selectedDevice.value = selectedDevice
            _availableResolutions.value = selectedDevice.supportedFormats?.toList() ?: emptyList()
            _resolution.value = selectDesiredResolution(
                selectedDevice.supportedFormats,
                mediaManager.call.state.settings.value?.video,
            )
            if (_resolution.value == null) {
                retryResolutionSelection()
            }

            if (!triggeredByFlip) {
                setup(force = true)
                startCapture()
            }
        }
    }

    private var setupCompleted: Boolean = false

    /**
     * Capture is called whenever you call enable()
     */
    internal fun startCapture() = synchronized(this) {
        safeCall {
            if (isCapturingVideo) {
                stopCapture()
            }

            val selectedDevice = _selectedDevice.value ?: return
            val selectedResolution = resolution.value ?: return

            // setup the camera 2 capturer
            videoCapturer = Camera2Capturer(mediaManager.context, selectedDevice.id, null)

            // initialize it
            videoCapturer.initialize(
                surfaceTextureHelper,
                mediaManager.context,
                mediaManager.videoSource.capturerObserver,
            )

            // and start capture
            videoCapturer.startCapture(
                selectedResolution.width,
                selectedResolution.height,
                selectedResolution.framerate.max,
            )
            isCapturingVideo = true
        }
    }

    /**
     * Stops capture if it's running
     */
    internal fun stopCapture() = synchronized(this) {
        safeCall {
            if (isCapturingVideo) {
                videoCapturer.stopCapture()
                isCapturingVideo = false
            }
        }
    }

    /**
     * Handle the setup of the camera manager and enumerator
     * You should only call this once the permissions have been granted
     */
    internal fun setup(force: Boolean = false): Result<Unit> = runCatching {
        if (setupCompleted && !force) {
            return@runCatching
        }

        initDeviceList()

        val devicesMatchingDirection = filterDevicesByAvailableDirection(devices, _direction.value)
        val selectedDevice = devicesMatchingDirection.firstOrNull()
        if (selectedDevice != null) {
            _selectedDevice.value = selectedDevice
            _resolution.value = selectDesiredResolution(
                selectedDevice.supportedFormats,
                mediaManager.call.state.settings.value?.video,
            )
            if (_resolution.value == null) {
                retryResolutionSelection()
            }
            _availableResolutions.value =
                selectedDevice.supportedFormats?.toList() ?: emptyList()

            surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread", eglBaseContext,
            )
            setupCompleted = true
        }
    }

    /**
     * Returns a list of camera devices filtered by the preferred direction (e.g., FRONT or BACK).
     * If no device matches the preferred direction, the original list is returned unfiltered.
     *
     * This ensures graceful fallback behavior on devices with non-standard or limited camera configurations.
     *
     * @param devices The full list of available camera devices.
     * @param preferredDirection The camera direction to prioritize.
     * @return A list of devices matching the preferred direction, or all devices if none match.
     */
    private fun filterDevicesByAvailableDirection(
        devices: List<CameraDeviceWrapped>,
        preferredDirection: CameraDirection,
    ): List<CameraDeviceWrapped> {
        return if (devices.any { it.direction == preferredDirection }) {
            devices.filter { it.direction == preferredDirection }
        } else {
            devices
        }
    }

    private fun initDeviceList() {
        cameraManager = mediaManager.context.getSystemService()
        enumerator = Camera2Enumerator(mediaManager.context)
        val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
        devices = sortDevices(cameraIds, cameraManager, enumerator)
    }

    /**
     * Creates a sorted list of camera devices
     *
     * @param cameraManager the system camera manager ([CameraManager].
     * @param enumerator the enumerator of cameras ([Camera2Enumerator]
     */
    internal fun sortDevices(
        ids: Array<String>,
        cameraManager: CameraManager?,
        enumerator: Camera2Enumerator,
    ): List<CameraDeviceWrapped> {
        val devices = mutableListOf<CameraDeviceWrapped>()

        for (id in ids) {
            try {
                createCameraDeviceWrapper(id, cameraManager, enumerator)?.let {
                    devices.add(it)
                }
            } catch (t: Throwable) {
                logger.e(t) { "Could not create camera device for camera with id: $id" }
            }
        }
        return devices.sortedBy { it.maxResolution }
    }

    /**
     * Gets the resolution that's closest to our target resolution
     */
    internal fun selectDesiredResolution(
        supportedFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>?,
        videoSettings: VideoSettingsResponse?,
    ): CameraEnumerationAndroid.CaptureFormat? {
        // needs the settings that we're going for
        // sort and get the one closest to 960
        val targetWidth = videoSettings?.targetResolution?.width ?: 1280
        val targetHeight = videoSettings?.targetResolution?.height ?: 720

        val matchingTarget =
            supportedFormats?.toList()
                ?.sortedBy { kotlin.math.abs(it.height - targetHeight) + kotlin.math.abs(it.width - targetWidth) }
        val selectedFormat = matchingTarget?.firstOrNull()
        logger.i { "selectDesiredResolution: $selectedFormat" }
        return selectedFormat
    }

    fun cleanup() {
        stopCapture()
        if (::videoCapturer.isInitialized) {
            videoCapturer.dispose()
        }
        if (::surfaceTextureHelper.isInitialized) {
            surfaceTextureHelper.dispose()
        }
        setupCompleted = false
    }

    private fun createCameraDeviceWrapper(
        id: String,
        cameraManager: CameraManager?,
        enumerator: Camera2Enumerator,
    ): CameraDeviceWrapped? {
        val characteristics = cameraManager?.getCameraCharacteristics(id)

        val direction = when (cameraCharacteristicsValidator.getLensFacing(characteristics) ?: -1) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraDirection.Front
            CameraCharacteristics.LENS_FACING_BACK -> CameraDirection.Back
            // Note: The camera device is an external camera, and has no fixed facing relative to the device's screen.
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraDirection.Back
            else -> null
        }
        val supportedFormats = enumerator.getSupportedFormats(id)
        val maxResolution = supportedFormats?.maxOfOrNull { it.width * it.height } ?: 0

        val isCameraUsable = cameraCharacteristicsValidator.isUsable(characteristics)

        if (!isCameraUsable) {
            logger.d { "Camera device $id is not usable" }
            return null
        }

        return CameraDeviceWrapped(
            id = id,
            direction = direction,
            characteristics = characteristics,
            supportedFormats = supportedFormats,
            maxResolution = maxResolution,
        )
    }

    private fun retryResolutionSelection(retries: Int = 5) {
        mediaManager.scope.launch {
            repeat(retries) { attempt ->
                delay((1000L * (attempt + 1)).coerceAtLeast(3000L))
                val selectedDevice = selectedDevice.value
                if (selectedDevice != null) {
                    val desired = selectDesiredResolution(
                        selectedDevice.supportedFormats,
                        mediaManager.call.state.settings.value?.video,
                    )
                    if (desired != null) {
                        _resolution.value = desired
                        startCapture()
                        return@launch
                    }
                }
            }
            logger.w { "Resolution selection failed after $retries retries" }
        }
    }
}

/**
 * Wrap all the audio/video interactions
 * This makes it easier to test our codebase
 *
 * This class knows about audio/ video.
 * It shouldn't be aware of webrtc tracks. Those are handled in the RtcSession
 *
 * @see RtcSession
 *
 * The Rtc session observes these stateflows and updates accordingly
 *
 * For development on this library also see:
 *
 * @see AudioSwitchHandler
 * @see AudioSwitch
 * @see BluetoothHeadsetManager
 */
class MediaManagerImpl(
    val context: Context,
    val call: Call,
    val scope: CoroutineScope,
    val eglBaseContext: EglBase.Context,
    @Deprecated("Use audioUsageProvider instead", replaceWith = ReplaceWith("audioUsageProvider"))
    val audioUsage: Int = defaultAudioUsage,
    val audioUsageProvider: (() -> Int) = { audioUsage },
    val useInBuiltAudioSwitch: Boolean = false,
) {
    internal val camera =
        CameraManager(this, eglBaseContext, DefaultCameraCharacteristicsValidator())
    internal val microphone = MicrophoneManager(this, audioUsage, audioUsageProvider)
    internal val speaker = SpeakerManager(this, microphone, audioUsageProvider = audioUsageProvider)
    internal val screenShare = ScreenShareManager(this, eglBaseContext)

    private val filterVideoProcessor =
        FilterVideoProcessor({ call.videoFilter }, { camera.surfaceTextureHelper })
    private val screenShareFilterVideoProcessor =
        FilterVideoProcessor({ null }, { screenShare.surfaceTextureHelper })

    // videoSource and videoTrack are nullable and recreated when factory changes (before joining)
    private var _videoSource: VideoSource? = null
    private var _screenShareVideoSource: VideoSource? = null
    private var _videoTrack: VideoTrack? = null
    private var _screenShareTrack: VideoTrack? = null

    val videoSource: VideoSource
        get() {
            if (_videoSource == null) {
                _videoSource = call.peerConnectionFactory.makeVideoSource(false, filterVideoProcessor)
            }
            return _videoSource!!
        }

    val screenShareVideoSource: VideoSource
        get() {
            if (_screenShareVideoSource == null) {
                _screenShareVideoSource = call.peerConnectionFactory.makeVideoSource(true, screenShareFilterVideoProcessor)
            }
            return _screenShareVideoSource!!
        }

    // for track ids we emulate the browser behaviour of random UUIDs, doing something different would be confusing
    // Todo : make videoTrack val in next major release and also move it out of MediaManager
    var videoTrack: VideoTrack
        get() {
            if (_videoTrack == null) {
                _videoTrack = call.peerConnectionFactory.makeVideoTrack(
                    source = videoSource,
                    trackId = UUID.randomUUID().toString(),
                )
            }
            return _videoTrack!!
        }
        set(value) {
            _videoTrack = value
        }

    // Todo : make screenShareTrack val in next major release and also move it out of MediaManager
    var screenShareTrack: VideoTrack
        get() {
            if (_screenShareTrack == null) {
                _screenShareTrack = call.peerConnectionFactory.makeVideoTrack(
                    source = screenShareVideoSource,
                    trackId = UUID.randomUUID().toString(),
                )
            }
            return _screenShareTrack!!
        }
        set(value) {
            _screenShareTrack = value
        }

    // audioSource and audioTrack are nullable and recreated when profile changes (before joining)
    private var _audioSource: AudioSource? = null
    private var _audioTrack: AudioTrack? = null

    val audioSource: AudioSource
        get() {
            if (_audioSource == null) {
                _audioSource = call.peerConnectionFactory.makeAudioSource(
                    buildAudioConstraints { microphone.audioBitrateProfile.value },
                )
            }
            return _audioSource!!
        }

    // for track ids we emulate the browser behaviour of random UUIDs, doing something different would be confusing
    // Todo : make audioTrack val in next major release and also move it out of MediaManager
    var audioTrack: AudioTrack
        get() {
            if (_audioTrack == null) {
                _audioTrack = call.peerConnectionFactory.makeAudioTrack(
                    source = audioSource,
                    trackId = UUID.randomUUID().toString(),
                )
            }
            return _audioTrack!!
        }
        set(value) {
            _audioTrack = value
        }

    /**
     * Disposes all tracks and sources without cleaning up camera/microphone infrastructure.
     * This is used when recreating the factory before joining.
     */
    internal fun disposeTracksAndSources() {
        _audioTrack?.dispose()
        _audioSource?.dispose()
        _videoTrack?.dispose()
        _videoSource?.dispose()
        _screenShareTrack?.dispose()
        _screenShareVideoSource?.dispose()

        // Clear references
        _audioTrack = null
        _audioSource = null
        _videoTrack = null
        _videoSource = null
        _screenShareTrack = null
        _screenShareVideoSource = null
    }

    fun cleanup() {
        // Dispose all tracks and sources
        disposeTracksAndSources()
        // Cleanup camera and microphone infrastructure
        camera.cleanup()
        microphone.cleanup()
    }
}

fun MediaStreamTrack.trySetEnabled(enabled: Boolean) = safeCall { setEnabled(enabled) }