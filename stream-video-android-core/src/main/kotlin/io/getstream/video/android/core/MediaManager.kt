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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.core.content.getSystemService
import io.getstream.android.video.generated.models.VideoSettingsResponse
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.call.video.FilterVideoProcessor
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
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaStreamTrack
import org.webrtc.SurfaceTextureHelper
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
                val device = createCameraDeviceWrapper(id, cameraManager, enumerator)
                devices.add(device)
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
    ): CameraDeviceWrapped {
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
    val audioUsage: Int = defaultAudioUsage,
) {
    private val filterVideoProcessor =
        FilterVideoProcessor({ call.videoFilter }, { camera.surfaceTextureHelper })
    private val screenShareFilterVideoProcessor =
        FilterVideoProcessor({ null }, { screenShare.surfaceTextureHelper })

    // source & tracks
    val videoSource =
        call.peerConnectionFactory.makeVideoSource(false, filterVideoProcessor)

    val screenShareVideoSource =
        call.peerConnectionFactory.makeVideoSource(true, screenShareFilterVideoProcessor)

    // for track ids we emulate the browser behaviour of random UUIDs, doing something different would be confusing
    var videoTrack = call.peerConnectionFactory.makeVideoTrack(
        source = videoSource,
        trackId = UUID.randomUUID().toString(),
    )

    var screenShareTrack = call.peerConnectionFactory.makeVideoTrack(
        source = screenShareVideoSource,
        trackId = UUID.randomUUID().toString(),
    )

    val audioSource = call.peerConnectionFactory.makeAudioSource(buildAudioConstraints())

    // for track ids we emulate the browser behaviour of random UUIDs, doing something different would be confusing
    var audioTrack = call.peerConnectionFactory.makeAudioTrack(
        source = audioSource,
        trackId = UUID.randomUUID().toString(),
    )

    internal val camera = CameraManager(this, eglBaseContext)
    internal val microphone = MicrophoneManager(this, audioUsage)
    internal val speaker = SpeakerManager(this, microphone)
    internal val screenShare = ScreenShareManager(this, eglBaseContext)

    fun cleanup() {
        videoSource.dispose()
        screenShareVideoSource.dispose()
        videoTrack.dispose()
        audioSource.dispose()
        audioTrack.dispose()
        camera.cleanup()
        microphone.cleanup()
    }
}

fun MediaStreamTrack.trySetEnabled(enabled: Boolean) = safeCall { setEnabled(enabled) }
