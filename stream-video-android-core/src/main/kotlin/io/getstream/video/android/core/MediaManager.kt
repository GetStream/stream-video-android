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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.getstream.android.video.generated.models.VideoSettingsResponse
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.audio.AudioHandler
import io.getstream.video.android.core.audio.AudioSwitchHandler
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.video.FilterVideoProcessor
import io.getstream.video.android.core.notifications.internal.service.telecom.VoipConnection
import io.getstream.video.android.core.screenshare.StreamScreenShareService
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.EglBase
import org.webrtc.MediaStreamTrack
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import stream.video.sfu.models.VideoDimension
import java.util.UUID

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
) {

    private val logger by taggedLogger("Media:SpeakerManager")

    private var priorVolume: Int? = null
    private val _volume = MutableStateFlow(initialVolume)
    val volume: StateFlow<Int?> = _volume

    /** The status of the audio */
    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    /** Represents whether the speakerphone is enabled */
    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

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
        microphoneManager.enforceSetup {
            val devices = devices.value
            if (enable) {
                val speaker =
                    devices.filterIsInstance<StreamAudioDevice.Speakerphone>().firstOrNull()
                selectedBeforeSpeaker = selectedDevice.value
                _speakerPhoneEnabled.value = true
                microphoneManager.select(speaker)
            } else {
                _speakerPhoneEnabled.value = false
                // swap back to the old one
                val defaultFallbackFromType = defaultFallback?.let {
                    devices.filterIsInstance(defaultFallback::class.java)
                }?.firstOrNull()
                val fallback =
                    defaultFallbackFromType ?: selectedBeforeSpeaker ?: devices.firstOrNull {
                        it !is StreamAudioDevice.Speakerphone
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
    }

    private val logger by taggedLogger("Media:ScreenShareManager")

    private val _status = MutableStateFlow<DeviceStatus>(DeviceStatus.NotSelected)
    val status: StateFlow<DeviceStatus> = _status

    public val isEnabled: StateFlow<Boolean> = _status.mapState { it is DeviceStatus.Enabled }

    private lateinit var screenCapturerAndroid: ScreenCapturerAndroid
    internal lateinit var surfaceTextureHelper: SurfaceTextureHelper
    private var setupCompleted = false
    private var isScreenSharing = false
    private var mediaProjectionPermissionResultData: Intent? = null

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

            isScreenSharing = true
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    fun enable(mediaProjectionPermissionResultData: Intent, fromUser: Boolean = true) {
        mediaManager.screenShareTrack.setEnabled(true)
        if (fromUser) {
            _status.value = DeviceStatus.Enabled
        }
        setup()
        startScreenShare(mediaProjectionPermissionResultData)
    }

    fun disable(fromUser: Boolean = true) {
        if (fromUser) {
            _status.value = DeviceStatus.Disabled
        }

        if (isScreenSharing) {
            mediaManager.screenShareTrack.setEnabled(false)
            screenCapturerAndroid.stopCapture()
            mediaManager.context.stopService(
                Intent(mediaManager.context, StreamScreenShareService::class.java),
            )
            isScreenSharing = false
        }
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
    val audioUsage: Int,
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

    /** Currently selected device */
    val selectedDevice: StateFlow<StreamAudioDevice?> = _selectedDevice

    private val _devices = MutableStateFlow<List<StreamAudioDevice>>(emptyList())

    /** List of available devices. */
    val devices: StateFlow<List<StreamAudioDevice>> = _devices

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
     * Select a specific device
     */
    fun select(device: StreamAudioDevice?) {
        enforceSetup {
            logger.i { "selecting device $device" }
            ifAudioHandlerInitialized { it.selectDevice(device) }
            _selectedDevice.value = device
        }
    }

    /**
     * List the devices, returns a stateflow with audio devices
     */
    fun listDevices(): StateFlow<List<StreamAudioDevice>> {
        setup()
        return devices
    }

    fun cleanup() {
        ifAudioHandlerInitialized { it.stop() }
        setupCompleted = false
    }

    fun canHandleDeviceSwitch() = audioUsage != AudioAttributes.USAGE_MEDIA

    // Internal logic
    internal fun setup(onAudioDevicesUpdate: (() -> Unit)? = null) {
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

        if (canHandleDeviceSwitch()) {
            audioHandler = VoipConnection.setDeviceListener { devices, selected ->
                logger.i {
                    "[setup] #telecom; listenForDevices. Selected: ${selected?.name}, available: ${devices.map { it.name }}"
                }

                _devices.value = devices
                _selectedDevice.value = selected

                capturedOnAudioDevicesUpdate?.invoke()
                capturedOnAudioDevicesUpdate = null
                setupCompleted = true
            }.also {
                it.start()
            }
        } else {
            logger.d { "[MediaManager#setup] usage is MEDIA, cannot handle device switch" }
        }
    }

    internal fun enforceSetup(actual: () -> Unit) = setup(onAudioDevicesUpdate = actual)

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
public class CameraManager(
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
            val device = devices.firstOrNull { it.direction == newDirection }
            device?.let { select(it.id, triggeredByFlip = true) }

            videoCapturer.switchCamera(null)
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
    internal fun setup(force: Boolean = false) {
        if (setupCompleted && !force) {
            return
        }

        initDeviceList()

        val devicesMatchingDirection = devices.filter { it.direction == _direction.value }
        val selectedDevice = devicesMatchingDirection.firstOrNull()
        if (selectedDevice != null) {
            _selectedDevice.value = selectedDevice
            _resolution.value = selectDesiredResolution(
                selectedDevice.supportedFormats,
                mediaManager.call.state.settings.value?.video,
            )
            _availableResolutions.value =
                selectedDevice.supportedFormats?.toList() ?: emptyList()

            surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread", eglBaseContext,
            )
            setupCompleted = true
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
        val selectedFormat = matchingTarget?.first()
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
