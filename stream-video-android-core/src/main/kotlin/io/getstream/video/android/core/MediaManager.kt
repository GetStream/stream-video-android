package io.getstream.video.android.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.model.CallSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*


sealed class DeviceStatus {
    object Disabled : DeviceStatus()
    object Enabled : DeviceStatus()
    object NoPermission : DeviceStatus()
}

class SpeakerManager(val mediaManager: MediaManagerImpl) {

    val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.Disabled)
    val status: StateFlow<DeviceStatus> = _status

    fun devices(): List<String> {
        return mediaManager.getCameraDevices()
    }

    fun select(deviceId: String) {
        mediaManager.selectCamera(deviceId)
    }

    fun enable() {
        mediaManager.setCameraEnabled(true)
    }

    fun disable() {
        mediaManager.setCameraEnabled(false)
    }
}

class MicrophoneManager(val mediaManager: MediaManagerImpl) {

    val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.Disabled)
    val status: StateFlow<DeviceStatus> = _status

    fun devices(): List<String> {
        return mediaManager.getCameraDevices()
    }

    fun select(deviceId: String) {
        mediaManager.selectCamera(deviceId)
    }

    fun enable() {
        mediaManager.setCameraEnabled(true)
    }

    fun disable() {
        mediaManager.setCameraEnabled(false)
    }
}



class CameraManager(val mediaManager: MediaManagerImpl) {

    val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.Disabled)
    val status: StateFlow<DeviceStatus> = _status

    fun flip() {
        mediaManager.flipCamera()
    }

    val _devices = MutableStateFlow<List<String>>(emptyList())
    val devices: StateFlow<List<String>> = _devices

//    fun devices(): List<String> {
//        return mediaManager.getCameraDevices()
//    }

    fun select(deviceId: String) {
        mediaManager.selectCamera(deviceId)
    }

    fun enable() {
        mediaManager.setCameraEnabled(true)
    }

    fun disable() {
        mediaManager.setCameraEnabled(false)
    }
}


/**
 * Wrap all the audio/video interactions
 * This makes it easier to test our codebase
 *
 * TODO: refactor code from these things into 1 media manager module
 * TODO: create a version of this class that spits out fake camera, microphone and speakers for testing
 *
 * - CallClientUtils (for constraints)
 * - AudioSwitchHandler
 * - AudioSwitch
 * - BluetoothHeadsetManager
 */
class MediaManagerImpl(val context: Context) {

    private var audioManager = context.getSystemService<AudioManager>()
    private var captureResolution: CameraEnumerationAndroid.CaptureFormat? = null
    private var isCapturingVideo: Boolean = false
    private var videoCapturer: Camera2Capturer? = null
    private val logger by taggedLogger("Call:MediaManagerImpl")
    private val cameraManager = context.getSystemService<CameraManager>()
    private val cameraEnumerator: CameraEnumerator by lazy {
        Camera2Enumerator(context)
    }

    val camera = CameraManager(this)

    // TODO: maybe merge microphone and speaker, not sure many apps allow you to split the concepts
    val microphone = MicrophoneManager(this)
    val speaker = SpeakerManager(this)

    val enumerator = Camera2Enumerator(context)




    fun getCameraDevices(): List<String> {

        val ids = cameraManager!!.cameraIdList
        val names = enumerator.deviceNames.toList()
        val manager = cameraManager

        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            val supportedFormats = enumerator.getSupportedFormats(names.first())
        }
        return names
    }

    public fun getAudioHandler(): io.getstream.video.android.core.audio.AudioSwitchHandler? {
        return audioHandler as? io.getstream.video.android.core.audio.AudioSwitchHandler
    }

    fun startCapturingLocalVideo(position: Int) {
        val capturer = videoCapturer as? Camera2Capturer ?: return
        val enumerator = cameraEnumerator as? Camera2Enumerator ?: return

        val frontCamera = enumerator.deviceNames.first {
            if (position == 0) {
                enumerator.isFrontFacing(it)
            } else {
                enumerator.isBackFacing(it)
            }
        }

        val supportedFormats = enumerator.getSupportedFormats(frontCamera) ?: emptyList()

        val resolution = supportedFormats.firstOrNull {
            (it.width == 720 || it.width == 480 || it.width == 360)
        } ?: return

        capturer.startCapture(resolution.width, resolution.height, 30)
        isCapturingVideo = true
        captureResolution = resolution
    }

    fun buildCameraCapturer(): VideoCapturer? {
        val manager = cameraManager ?: return null

        val ids = manager.cameraIdList
        var foundCamera = false
        var cameraId = ""

        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT) {
                foundCamera = true
                cameraId = id
            }
        }

        if (!foundCamera && ids.isNotEmpty()) {
            cameraId = ids.first()
        }

        val camera2Capturer = Camera2Capturer(context, cameraId, null)
        videoCapturer = camera2Capturer
        return camera2Capturer
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

    fun flipCamera() {
        TODO("Not yet implemented")
    }

    fun setCameraEnabled(b: Boolean) {
        TODO("Not yet implemented")
    }

    fun selectCamera(cameraId: String) {
        val camera2Capturer = Camera2Capturer(context, cameraId, null)
        val capturer = camera2Capturer as? Camera2Capturer

        val supportedFormats = enumerator.getSupportedFormats(cameraId) ?: emptyList()

        val resolution = supportedFormats.firstOrNull {
            (it.width == 720 || it.width == 480 || it.width == 360)
        }

        capturer!!.startCapture(resolution!!.width, resolution.height, 30)
    }

}