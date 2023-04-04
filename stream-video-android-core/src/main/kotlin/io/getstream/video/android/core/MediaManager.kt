package io.getstream.video.android.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator


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

    val camera = CameraManager(this)

    // TODO: maybe merge microphone and speaker, not sure many apps allow you to split the concepts
    val microphone = MicrophoneManager(this)
    val speaker = SpeakerManager(this)

    val enumerator = Camera2Enumerator(context)
    val cameraManager = context.getSystemService<CameraManager>()



    fun getCameraDevices(): List<String> {

        val ids = cameraManager!!.cameraIdList
        val names = enumerator.deviceNames.toList()

        for (id in ids) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

            val supportedFormats = enumerator.getSupportedFormats(names.first())
        }
        return names
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