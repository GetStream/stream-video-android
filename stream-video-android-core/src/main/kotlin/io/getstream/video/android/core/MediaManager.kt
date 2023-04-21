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
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.CapturerObserver
import org.webrtc.EglBase
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import org.webrtc.VideoSource

sealed class DeviceStatus {
    object Disabled : DeviceStatus()
    object Enabled : DeviceStatus()
}


data class CameraDeviceWrapped(
    val id: String,

    val characteristics: CameraCharacteristics?,
    val supportedFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>?,
    val maxResolution: Int,
    val direction: CameraDirection?
) {

}

class SpeakerManager(val mediaManager: MediaManagerImpl) {

    val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.Disabled)
    val status: StateFlow<DeviceStatus> = _status

    val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice

    val _devices = MutableStateFlow<List<String>>(emptyList())
    val devices: StateFlow<List<String>> = _devices

    val _speakerPhoneEnabled = MutableStateFlow(false)
    val speakerPhoneEnabled: StateFlow<Boolean> = _speakerPhoneEnabled

    fun devices(): List<String> {
        return mediaManager.getCameraDevices()
    }

    fun select(deviceId: String) {
        mediaManager.selectCamera(deviceId)
    }

    fun enable() {
        mediaManager.setSpeakerphoneEnabled(true)
    }

    fun setVolume(volumeLevel: Long) {
    }

    fun setSpeakerPhone(speakerPhone: Boolean) {
        mediaManager.setSpeakerphoneEnabled(speakerPhone)
        _speakerPhoneEnabled.value = speakerPhone
    }

    fun setEnabled(enabled: Boolean) {
        mediaManager.setCameraEnabled(enabled)
    }

    fun disable() {
        mediaManager.setCameraEnabled(false)
    }
}

class MicrophoneManager(val mediaManager: MediaManagerImpl) {

    val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.Disabled)
    val status: StateFlow<DeviceStatus> = _status

    val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice

    val _devices = MutableStateFlow<List<String>>(emptyList())
    val devices: StateFlow<List<String>> = _devices

    fun devices(): List<String> {
        return emptyList()
    }

    fun select(deviceId: String) {
    }
    fun setEnabled(enabled: Boolean) {
    }
    fun startCapture() {
    }

    fun enable() {
    }

    fun disable() {
    }
}

class AudioManager(val context: Context) {
    private val logger by taggedLogger("Media:AudioManager")

    var audioManager = context.getSystemService<AudioManager>()
    val audioHandler = AudioSwitchHandler(context)

//    val audioSource = clientImpl.peerConnectionFactory.makeAudioSource(audioConstraints)
//    val audioTrack = clientImpl.peerConnectionFactory.makeAudioTrack(
//        source = audioSource, trackId = "audioTrack"
//    )

    init {
        audioHandler.start()
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        val speakerOn = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: throw java.lang.IllegalStateException("No devices found")
            val deviceType = if (speakerOn) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }

            val device = devices.firstOrNull { it.type == deviceType } ?: throw java.lang.IllegalStateException("No devices found")

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }

        // listing devices..
        val allDevices =  audioHandler.availableAudioDevices
    }

    fun setDevice(device: AudioDevice) {
        audioHandler.selectDevice(device)
    }
//
//    fun setSpeaker(speakerOn: Boolean = true) {
//        val activeDevice = allDevices.firstOrNull {
//            if (true) {
//                it.name.contains("speaker", true)
//            } else {
//                !it.name.contains("speaker", true)
//            }
//        }
//    }
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
public class CameraManager(public val mediaManager: MediaManagerImpl, eglBaseContext: EglBase.Context, defaultCameraDirection: CameraDirection = CameraDirection.Front) {
    private lateinit var devices: List<CameraDeviceWrapped>
    private var isCapturingVideo: Boolean = false
    private lateinit var videoCapturer: Camera2Capturer
    private lateinit var enumerator: Camera2Enumerator
    private var cameraManager: CameraManager? = null
    private val logger by taggedLogger("Media:CameraManager")

    /** The status of the camera. enabled or disabled */
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.Disabled)
    public val status: StateFlow<DeviceStatus> = _status

    /** if we're using the front facing or back facing camera */
    private val _direction = MutableStateFlow<CameraDirection>(defaultCameraDirection)
    public val direction: StateFlow<CameraDirection> = _direction


    private val _selectedDevice = MutableStateFlow<CameraDeviceWrapped?>(null)
    public val selectedDevice: StateFlow<CameraDeviceWrapped?> = _selectedDevice

    private val _resolution = MutableStateFlow<CameraEnumerationAndroid.CaptureFormat?>(null)
    public val resolution: StateFlow<CameraEnumerationAndroid.CaptureFormat?> = _resolution

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

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            enable()
        } else {
            disable()
        }
    }

    fun disable() {
        // 1. update our local state
        // 2. update the track enabled status
        // 3. Rtc listens and sends the update mute state request
        _status.value = DeviceStatus.Disabled
        mediaManager.videoTrack.setEnabled(false)
        videoCapturer.stopCapture()
    }

    /**
     * Flips the camera
     */
    fun flip() {
        setup()
        val newDirection = when (_direction.value) {
            CameraDirection.Front -> CameraDirection.Back
            CameraDirection.Back -> CameraDirection.Front
        }
        val device = devices.first { it.direction == newDirection }
        select(device.id, false)

        videoCapturer?.switchCamera(null)
    }

    /**
     * Selects a specific device
     */
    fun select(deviceId: String, startCapture: Boolean = false) {
        val selectedDevice = devices.first { it.id == deviceId }
        _direction.value = selectedDevice.direction ?: CameraDirection.Back
        _selectedDevice.value = selectedDevice
        _resolution.value = selectDesiredResolution(selectedDevice.supportedFormats, 960)

        if (startCapture) {
            startCapture()
        }
    }

    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(
            "CaptureThread", eglBaseContext
        )
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
            videoCapturer?.initialize(
                surfaceTextureHelper,
                mediaManager.context,
                mediaManager
            )
        }

        // and start capture
        runBlocking(mediaManager.scope.coroutineContext) {
            videoCapturer!!.startCapture(selectedResolution.width, selectedResolution.height, selectedResolution.framerate.max)
        }
        isCapturingVideo = true

    }

    /**
     * Stops capture if it's running
     */
    internal fun stopCapture() {
        if (isCapturingVideo) {
            videoCapturer?.stopCapture()
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
        _resolution.value = selectDesiredResolution(selectedDevice.supportedFormats, 960)

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
    internal fun selectDesiredResolution(supportedFormats: MutableList<CameraEnumerationAndroid.CaptureFormat>?, targetResolution:Int = 960,): CameraEnumerationAndroid.CaptureFormat? {
        // needs the settings that we're going for
        // sort and get the one closest to 960
        val sorted = supportedFormats?.toList()?.sortedBy { kotlin.math.abs(it.height - targetResolution) }
        return sorted?.first()
    }

    /**
     * When the track becomes available TODO: remove htis
     */
    internal fun setVideoSource(videoSource: VideoSource) {
        this.videoSource = videoSource
        runBlocking(scope.coroutineContext) {
            videoCapturer?.initialize(
                surfaceTextureHelper,
                context,
                videoSource.capturerObserver
            )
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
 * Also see:
 *
 * @see AudioSwitchHandler
 * @see AudioSwitch
 * @see BluetoothHeadsetManager
 */
// TODO: add the call, or the settings object
class MediaManagerImpl(val context: Context, val call: Call, val scope: CoroutineScope, val eglBaseContext: EglBase.Context) : CapturerObserver {

    private var audioManager = context.getSystemService<AudioManager>()
    internal var captureResolution: CameraEnumerationAndroid.CaptureFormat? = null
    private var isCapturingVideo: Boolean = false
    var videoCapturer: Camera2Capturer? = null
    private val logger by taggedLogger("Call:MediaManagerImpl")



    val videoSource = call.clientImpl.peerConnectionFactory.makeVideoSource(false)
    val audioSource = call.clientImpl.peerConnectionFactory.makeAudioSource(buildAudioConstraints())
    val audioTrack = call.clientImpl.peerConnectionFactory.makeAudioTrack(
        source = audioSource, trackId = "audioTrack"
    )
    val videoTrack = call.clientImpl.peerConnectionFactory.makeVideoTrack(
        source = videoSource, trackId = "videoTrack"
    )

    val camera = CameraManager(this, eglBaseContext)

    val microphone = MicrophoneManager(this)
    val speaker = SpeakerManager(this)

    val enumerator = Camera2Enumerator(context)



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

    fun selectAudioDevice(device: io.getstream.video.android.core.audio.AudioDevice) {
        logger.d { "[selectAudioDevice] #sfu; device: $device" }
        val handler = getAudioHandler() ?: return

        handler.selectDevice(device)
    }

    public fun getAudioHandler(): AudioSwitchHandler? {
        return audioHandler as? AudioSwitchHandler
    }



    fun getAudioDevices(): List<io.getstream.video.android.core.audio.AudioDevice> {
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

    override fun onCapturerStarted(success: Boolean) {
        //
    }

    override fun onCapturerStopped() {
        //
    }

    override fun onFrameCaptured(frame: VideoFrame?) {
        //
    }




}
