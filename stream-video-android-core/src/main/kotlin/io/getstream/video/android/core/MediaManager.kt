/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioDevice
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.model.CallSettings
import io.getstream.video.android.core.utils.buildAudioConstraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toImmutableList
import org.openapitools.client.models.VideoSettings
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.EglBase
import org.webrtc.SurfaceTextureHelper

sealed class DeviceStatus {
    object NotSelected : DeviceStatus()
    object Disabled : DeviceStatus()
    object Enabled : DeviceStatus()
}

data class CameraDeviceWrapped(
    val id: String,
    val characteristics: CameraCharacteristics?,
    val supportedFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>?,
    val maxResolution: Int,
    val direction: CameraDirection?
)

class SpeakerManager(val mediaManager: MediaManagerImpl, val microphoneManager: MicrophoneManager, val initialVolume: Int? = null) {

    private var priorVolume: Int? = null
    private val _volume = MutableStateFlow<Int?>(initialVolume)
    val volume: StateFlow<Int?> = _volume

    val selectedDevice: StateFlow<AudioDevice?> = microphoneManager.selectedDevice

    val devices: StateFlow<List<AudioDevice>> = microphoneManager.devices

    private val _speakerPhoneEnabled = MutableStateFlow(false)
    val speakerPhoneEnabled: StateFlow<Boolean> = _speakerPhoneEnabled

    internal var selectedBeforeSpeaker: AudioDevice? = null

    fun enableSpeakerPhone() {
        setSpeakerPhone(true)
    }

    fun disableSpeakerPhone() {
        setSpeakerPhone(false)
    }

    /** enables or disables the speakerphone */
    fun setSpeakerPhone(enable: Boolean) {
        microphoneManager.setup()
        val devices = devices.value
        if (enable) {
            val speaker = devices.filterIsInstance<AudioDevice.Speakerphone>().firstOrNull()
            selectedBeforeSpeaker = selectedDevice.value
            _speakerPhoneEnabled.value = true
            microphoneManager.select(speaker)
        } else {
            _speakerPhoneEnabled.value = false
            // swap back to the old one
            val fallback =
                selectedBeforeSpeaker ?: devices.firstOrNull { it !is AudioDevice.Speakerphone }
            microphoneManager.select(fallback)
        }
    }

    /**
     * Set the volume as a percentage, 0-100
     */
    fun setVolume(volumePercentage: Int) {
        microphoneManager.setup()
        microphoneManager.audioManager?.let {
            val max = it.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val level = max / 100 * volumePercentage
            _volume.value = volumePercentage
            it.setStreamVolume(AudioManager.STREAM_VOICE_CALL, level, 0)
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
class MicrophoneManager(val mediaManager: MediaManagerImpl) {
    private lateinit var audioHandler: AudioSwitchHandler
    internal var audioManager: AudioManager? = null

    private val logger by taggedLogger("Media:MicrophoneManager")

    /** The status of the audio */
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    private val _selectedDevice = MutableStateFlow<AudioDevice?>(null)
    val selectedDevice: StateFlow<AudioDevice?> = _selectedDevice

    private val _devices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val devices: StateFlow<List<AudioDevice>> = _devices

    internal var priorStatus : DeviceStatus? = null

    /** Enable the audio, the rtc engine will automatically inform the SFU */
    fun enable() {
        setup()
        _status.value = DeviceStatus.Enabled
        mediaManager.audioTrack.setEnabled(true)
    }

    fun pause() {
        // pause the microphone, and when resuming switched back to the previous state
        priorStatus = _status.value
        disable()
    }

    fun resume() {
        priorStatus?.let {
            if (it == DeviceStatus.Enabled) {
                enable()
            }
        }
    }

    /** Disable the audio track. Audio is still captured, but not send.
     * This allows for the "you are muted" toast to indicate you are talking while muted */
    fun disable() {
        _status.value = DeviceStatus.Disabled
        mediaManager.audioTrack.setEnabled(false)
    }

    /**
     * Enable or disable the microphone
     */
    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            enable()
        } else {
            disable()
        }
    }

    /**
     * Select a specific device
     */
    fun select(device: AudioDevice?) {
        audioHandler.selectDevice(device)
        _selectedDevice.value = device
    }

    /**
     * List the devices, returns a stateflow with audio devices
     */
    fun listDevices(): StateFlow<List<AudioDevice>> {
        setup()
        return devices
    }

    internal fun setup() {
        if (setupCompleted) return

        audioManager = mediaManager.context.getSystemService<AudioManager>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            audioManager?.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
        }
        audioHandler = AudioSwitchHandler(mediaManager.context)
        val devices = audioHandler.availableAudioDevices
        _devices.value = devices

        setupCompleted = true
    }

    fun cleanup() {
        audioHandler.stop()
        setupCompleted = false
    }

    private var setupCompleted: Boolean = false
}

public sealed class CameraDirection {
    public object Front : CameraDirection()
    public object Back : CameraDirection()
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
public class CameraManager(
    public val mediaManager: MediaManagerImpl,
    public val eglBaseContext: EglBase.Context,
    defaultCameraDirection: CameraDirection = CameraDirection.Front
) {
    private var priorStatus: DeviceStatus? = null
    private lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private lateinit var devices: List<CameraDeviceWrapped>
    private var isCapturingVideo: Boolean = false
    private lateinit var videoCapturer: Camera2Capturer
    private lateinit var enumerator: Camera2Enumerator
    private var cameraManager: CameraManager? = null
    private val logger by taggedLogger("Media:CameraManager")

    /** The status of the camera. enabled or disabled */
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    public val status: StateFlow<DeviceStatus> = _status

    /** if we're using the front facing or back facing camera */
    private val _direction = MutableStateFlow<CameraDirection>(defaultCameraDirection)
    public val direction: StateFlow<CameraDirection> = _direction

    private val _selectedDevice = MutableStateFlow<CameraDeviceWrapped?>(null)
    public val selectedDevice: StateFlow<CameraDeviceWrapped?> = _selectedDevice

    private val _resolution = MutableStateFlow<CameraEnumerationAndroid.CaptureFormat?>(null)
    public val resolution: StateFlow<CameraEnumerationAndroid.CaptureFormat?> = _resolution

    private val _availableResolutions: MutableStateFlow<List<CameraEnumerationAndroid.CaptureFormat>> =
        MutableStateFlow(emptyList())
    public val availableResolutions: StateFlow<List<CameraEnumerationAndroid.CaptureFormat>> =
        _availableResolutions

    public fun listDevices(): List<CameraDeviceWrapped> {
        setup()
        return devices
    }

    fun enable() {
        setup()
        // 1. update our local state
        // 2. update the track enabled status
        // 3. Rtc listens and sends the update mute state request
        _status.value = DeviceStatus.Enabled
        mediaManager.videoTrack.setEnabled(true)
        startCapture()
    }

    fun pause() {
        // pause the camera, and when resuming switched back to the previous state
        priorStatus = _status.value
        disable()
    }

    fun resume() {
        priorStatus?.let {
            if (it == DeviceStatus.Enabled) {
                enable()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            enable()
        } else {
            disable()
        }
    }

    fun disable() {
        if (isCapturingVideo) {
            // 1. update our local state
            // 2. update the track enabled status
            // 3. Rtc listens and sends the update mute state request
            _status.value = DeviceStatus.Disabled
            mediaManager.videoTrack.setEnabled(false)
            videoCapturer.stopCapture()
            isCapturingVideo = false
        }
    }

    /**
     * Flips the camera
     */
    fun flip() {
        if (isCapturingVideo) {
            setup()
            val newDirection = when (_direction.value) {
                CameraDirection.Front -> CameraDirection.Back
                CameraDirection.Back -> CameraDirection.Front
            }
            val device = devices.first { it.direction == newDirection }
            select(device.id, false)

            videoCapturer.switchCamera(null)
        }
    }

    fun select(deviceId: String, startCapture: Boolean = false) {
        val selectedDevice = devices.first { it.id == deviceId }
        _direction.value = selectedDevice.direction ?: CameraDirection.Back
        _selectedDevice.value = selectedDevice
        _availableResolutions.value =
            selectedDevice.supportedFormats?.toImmutableList() ?: emptyList()
        _resolution.value = selectDesiredResolution(selectedDevice.supportedFormats, mediaManager.call.state.settings.value?.video)

        if (startCapture) {
            startCapture()
        }
    }

    private var setupCompleted: Boolean = false

    /**
     * Capture is called whenever you call enable()
     */
    internal fun startCapture() {
        val selectedDevice = _selectedDevice.value ?: return
        val selectedResolution = resolution.value ?: return

        // setup the camera 2 capturer
        videoCapturer = Camera2Capturer(mediaManager.context, selectedDevice.id, null)

        // initialize it
        runBlocking(mediaManager.scope.coroutineContext) {
            videoCapturer.initialize(
                surfaceTextureHelper,
                mediaManager.context,
                mediaManager.videoSource.capturerObserver
            )
        }

        // and start capture
        runBlocking(mediaManager.scope.coroutineContext) {
            videoCapturer.startCapture(
                selectedResolution.width,
                selectedResolution.height,
                selectedResolution.framerate.max
            )
        }
        isCapturingVideo = true
    }

    /**
     * Stops capture if it's running
     */
    internal fun stopCapture() {
        if (isCapturingVideo) {
            videoCapturer.stopCapture()
            isCapturingVideo = false
        }
    }

    /**
     * Handle the setup of the camera manager and enumerator
     * You should only call this once the permissions have been granted
     */
    internal fun setup() {
        if (setupCompleted) {
            return
        }
        cameraManager = mediaManager.context.getSystemService()
        enumerator = Camera2Enumerator(mediaManager.context)
        devices = sortDevices()
        val devicesMatchingDirection = devices.filter { it.direction == _direction.value }
        val selectedDevice = devicesMatchingDirection.first()
        _selectedDevice.value = selectedDevice
        _resolution.value = selectDesiredResolution(selectedDevice.supportedFormats, mediaManager.call.state.settings.value?.video)
        _availableResolutions.value =
            selectedDevice.supportedFormats?.toImmutableList() ?: emptyList()

        surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread", eglBaseContext
        )

        setupCompleted = true
    }

    /**
     * Creates a sorted list of camera devices
     */
    internal fun sortDevices(): List<CameraDeviceWrapped> {
        val devices = mutableListOf<CameraDeviceWrapped>()

        val ids = cameraManager?.cameraIdList ?: emptyArray()

        for (id in ids) {
            val characteristics = cameraManager?.getCameraCharacteristics(id)

            val direction = when (characteristics?.get(CameraCharacteristics.LENS_FACING) ?: -1) {
                CameraCharacteristics.LENS_FACING_FRONT -> CameraDirection.Front
                CameraCharacteristics.LENS_FACING_BACK -> CameraDirection.Back
                // Note: The camera device is an external camera, and has no fixed facing relative to the device's screen.
                CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraDirection.Back
                else -> null
            }

            val supportedFormats = enumerator.getSupportedFormats(id)

            val maxResolution = supportedFormats?.maxOfOrNull { it.width * it.height } ?: 0
            val device = CameraDeviceWrapped(
                id = id,
                direction = direction,
                characteristics = characteristics,
                supportedFormats = supportedFormats,
                maxResolution = maxResolution
            )
            devices.add(device)
        }
        return devices.sortedBy { it.maxResolution }
    }

    /**
     * Gets the resolution that's closest to our target resolution
     */
    internal fun selectDesiredResolution(
        supportedFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>?,
        videoSettings: VideoSettings?,
    ): CameraEnumerationAndroid.CaptureFormat? {
        // needs the settings that we're going for
        // sort and get the one closest to 960
        var targetHeight = videoSettings?.targetResolution?.height ?: 720
        var targetWidth = videoSettings?.targetResolution?.width ?: 1280

//        targetHeight = 1908
//        targetWidth = 3392

        val matchingTarget =
            supportedFormats?.toList()?.sortedBy { kotlin.math.abs(it.height - targetHeight) + kotlin.math.abs(it.width - targetWidth) }
        val selectedFormat = matchingTarget?.first()
        logger.i { "selectDesiredResolution: $selectedFormat" }
        return selectedFormat
    }

    fun cleanup() {
        stopCapture()
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        setupCompleted = false
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
// TODO: add the call, or the settings object
class MediaManagerImpl(
    val context: Context,
    val call: Call,
    val scope: CoroutineScope,
    val eglBaseContext: EglBase.Context
) {
    private val logger by taggedLogger("Call:MediaManagerImpl")
    private var audioManager = context.getSystemService<AudioManager>()

    // source & tracks
    val videoSource = call.clientImpl.peerConnectionFactory.makeVideoSource(false)
    val videoTrack = call.clientImpl.peerConnectionFactory.makeVideoTrack(
        source = videoSource, trackId = "videoTrack"
    )

    val audioSource = call.clientImpl.peerConnectionFactory.makeAudioSource(buildAudioConstraints())
    val audioTrack = call.clientImpl.peerConnectionFactory.makeAudioTrack(
        source = audioSource, trackId = "audioTrack"
    )

    val camera = CameraManager(this, eglBaseContext)
    val microphone = MicrophoneManager(this)
    val speaker = SpeakerManager(this, microphone)

    fun setSpeakerphoneEnabled(isEnabled: Boolean) {
        val devices = getAudioDevices()

        val activeDevice = devices.firstOrNull {
            if (isEnabled) {
                it.name.contains("speaker", true)
            } else {
                !it.name.contains("speaker", true)
            }
        }

        getAudioHandler()?.selectDevice(activeDevice)
    }

    fun selectAudioDevice(device: AudioDevice) {
        logger.d { "[selectAudioDevice] #sfu; device: $device" }
        val handler = getAudioHandler() ?: return

        handler.selectDevice(device)
    }

    public fun getAudioHandler(): AudioSwitchHandler? {
        return audioHandler as? AudioSwitchHandler
    }

    fun getAudioDevices(): List<AudioDevice> {
        logger.d { "[getAudioDevices] #sfu; no args" }
        val handler = getAudioHandler() ?: return emptyList()

        return handler.availableAudioDevices
    }

    internal val audioHandler: AudioHandler by lazy {
        AudioSwitchHandler(context)
    }

    internal fun setupAudio(callSettings: CallSettings) {
        logger.d { "[setupAudio] #sfu; no args" }
        audioHandler.start()
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val deviceType = if (callSettings.speakerOn) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }

            val device = devices.firstOrNull { it.type == deviceType } ?: return

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }
    }

    fun cleanup() {
        videoSource.dispose()
        videoTrack.dispose()
        audioSource.dispose()
        audioTrack.dispose()
        camera.cleanup()
        microphone.cleanup()
    }
}
