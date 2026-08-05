/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.components

import android.content.Intent
import io.getstream.android.video.generated.models.AudioSettingsResponse
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.VideoSettingsResponse
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.CameraDirection
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.utils.SoundInputProcessor
import io.getstream.video.android.core.utils.RampValueUpAndDownHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples

/**
 * Owns the media pipeline for a call: the [StreamPeerConnectionFactory] lifecycle, the
 * [MediaManagerImpl] (camera / microphone / speaker / screen share), audio-level monitoring,
 * settings-driven device initialisation and screen sharing.
 *
 * @param eglBase provider for the shared EGL context; invoked lazily so the native context
 * isn't created until the media manager / factory actually needs it.
 * @param mediaManagerFactory creates the [MediaManagerImpl]; owned by the Call facade so the
 * public type's `call` dependency never leaks into this component.
 */
internal class CallMediaManager(
    private val type: String,
    private val id: String,
    private val clientImpl: StreamVideoClient,
    private val scope: CoroutineScope,
    private val state: CallState,
    private val sessionManager: CallSessionManager,
    private val eglBase: () -> EglBase,
    private val mediaManagerFactory: MediaManagerFactory,
) {
    private val logger by taggedLogger("Call:MediaManager:$type:$id")

    private val soundInputProcessor = SoundInputProcessor(thresholdCrossedCallback = {
        if (!mediaManager.microphone.isEnabled.value) {
            state.markSpeakingAsMuted()
        }
    })
    private val audioLevelOutputHelper = RampValueUpAndDownHelper()

    /** Smoothed local microphone volume level (0..1). */
    val localMicrophoneAudioLevel: StateFlow<Float> = audioLevelOutputHelper.currentLevel

    // peerConnectionFactory is nullable and recreated when audioBitrateProfile changes (before joining)
    private var _peerConnectionFactory: StreamPeerConnectionFactory? = null

    var peerConnectionFactory: StreamPeerConnectionFactory
        get() {
            if (_peerConnectionFactory == null) {
                _peerConnectionFactory = StreamPeerConnectionFactory(
                    context = clientImpl.context,
                    audioProcessing = clientImpl.audioProcessing,
                    audioUsage = clientImpl.callServiceConfigRegistry.get(type).audioUsage,
                    audioUsageProvider = { clientImpl.callServiceConfigRegistry.get(type).audioUsage },
                    audioBitrateProfileProvider = { mediaManager.microphone.audioBitrateProfile.value },
                    sharedEglBaseProvider = { eglBase() },
                    webRtcLoggingLevel = clientImpl.loggingLevel.webRtcLoggingLevel,
                )
            }
            return _peerConnectionFactory!!
        }
        set(value) {
            _peerConnectionFactory = value
        }

    val mediaManager by lazy {
        mediaManagerFactory.create(
            audioUsage = clientImpl.callServiceConfigRegistry.get(type).audioUsage,
            audioUsageProvider = { clientImpl.callServiceConfigRegistry.get(type).audioUsage },
        )
    }

    /** Starts streaming smoothed microphone audio levels into [localMicrophoneAudioLevel]. */
    fun startAudioLevelMonitoring() {
        scope.launch {
            soundInputProcessor.currentAudioLevel.collect {
                audioLevelOutputHelper.rampToValue(it)
            }
        }
    }

    fun processAudioSample(audioSample: AudioSamples) {
        soundInputProcessor.processSoundInput(audioSample.data)
    }

    /**
     * Checks if the audioBitrateProfile has changed since the factory was created,
     * and recreates the factory if needed. This should only be called before joining.
     *
     * If the factory hasn't been created yet, it will be created with the current profile
     * when first accessed, so no recreation is needed.
     */
    fun ensureFactoryMatchesAudioProfile() {
        val factory = _peerConnectionFactory

        // If factory hasn't been created yet, it will be created with current profile automatically
        if (factory == null) {
            return
        }

        // Check if current profile differs from the profile used to create the factory
        val factoryProfile = factory.audioBitrateProfile
        val currentProfile = mediaManager.microphone.audioBitrateProfile.value

        if (factoryProfile != null && currentProfile != factoryProfile) {
            logger.i {
                "Audio bitrate profile changed from $factoryProfile to $currentProfile. " +
                    "Recreating factory before joining."
            }
            recreateFactoryAndAudioTracks()
        }
    }

    /**
     * Recreates peerConnectionFactory, audioSource, audioTrack, videoSource and videoTrack
     * with the current audioBitrateProfile. This should only be called before the call is joined.
     */
    fun recreateFactoryAndAudioTracks() {
        val wasMicrophoneEnabled = mediaManager.microphone.status.value is DeviceStatus.Enabled
        val wasCameraEnabled = mediaManager.camera.status.value is DeviceStatus.Enabled

        // Dispose all tracks and sources first
        mediaManager.disposeTracksAndSources()

        // Recreate the factory (which will use the new audioBitrateProfile)
        recreatePeerConnectionFactory()

        // Re-enable tracks if they were enabled
        if (wasMicrophoneEnabled) {
            // audioTrack will be recreated on next access, then we enable it
            mediaManager.microphone.enable(fromUser = false)
        }
        if (wasCameraEnabled) {
            // videoTrack will be recreated on next access, then we enable it
            mediaManager.camera.enable(fromUser = false)
        }
    }

    /**
     * Recreates peerConnectionFactory with the current audioBitrateProfile.
     * This should only be called before the call is joined.
     */
    fun recreatePeerConnectionFactory() {
        _peerConnectionFactory?.dispose()
        _peerConnectionFactory = null
        // Next access to peerConnectionFactory will recreate it with current profile
    }

    /** Applies server-provided call settings to the local media manager. */
    fun updateMediaManagerFromSettings(callSettings: CallSettingsResponse) {
        val camera = mediaManager.camera
        val microphone = mediaManager.microphone
        val speaker = mediaManager.speaker

        // Speaker
        if (speaker.status.value is DeviceStatus.NotSelected) {
            val enableSpeaker =
                if (callSettings.video.cameraDefaultOn || camera.status.value is DeviceStatus.Enabled) {
                    // if camera is enabled then enable speaker. Eventually this should
                    // be a new audio.defaultDevice setting returned from backend
                    true
                } else {
                    callSettings.audio.defaultDevice == AudioSettingsResponse.DefaultDevice.Speaker ||
                        callSettings.audio.speakerDefaultOn
                }

            speaker.setEnabled(enabled = enableSpeaker)
        }

        monitorHeadset()

        // Camera
        if (camera.status.value is DeviceStatus.NotSelected) {
            val defaultDirection =
                if (callSettings.video.cameraFacing == VideoSettingsResponse.CameraFacing.Front) {
                    CameraDirection.Front
                } else {
                    CameraDirection.Back
                }
            camera.setDirection(defaultDirection)
            camera.setEnabled(callSettings.video.cameraDefaultOn)
        }

        // Mic
        if (microphone.status.value == DeviceStatus.NotSelected) {
            val enabled = callSettings.audio.micDefaultOn
            microphone.setEnabled(enabled)
        }
    }

    private fun monitorHeadset() {
        val microphone = mediaManager.microphone
        microphone.devices.onEach { availableDevices ->
            logger.d {
                "[monitorHeadset] new available devices, prev selected: ${microphone.nonHeadsetFallbackDevice}"
            }

            val bluetoothHeadset =
                availableDevices.find { it is StreamAudioDevice.BluetoothHeadset }
            val wiredHeadset = availableDevices.find { it is StreamAudioDevice.WiredHeadset }

            if (bluetoothHeadset != null) {
                logger.d { "[monitorHeadset] BT headset selected" }
                microphone.select(bluetoothHeadset)
            } else if (wiredHeadset != null) {
                logger.d { "[monitorHeadset] wired headset found" }
                microphone.select(wiredHeadset)
            } else {
                logger.d { "[monitorHeadset] no headset found" }

                microphone.nonHeadsetFallbackDevice?.let { deviceBeforeHeadset ->
                    logger.d { "[monitorHeadset] before device selected" }
                    microphone.select(deviceBeforeHeadset)
                }
            }
        }.launchIn(scope)
    }

    fun startScreenSharing(
        mediaProjectionPermissionResultData: Intent,
        includeAudio: Boolean = false,
    ) {
        if (state.ownCapabilities.value.contains(OwnCapability.Screenshare)) {
            sessionManager.session.value?.setScreenShareTrack()
            mediaManager.screenShare.enable(
                mediaProjectionPermissionResultData,
                includeAudio = includeAudio,
            )
        } else {
            logger.w { "Can't start screen sharing - user doesn't have wnCapability.Screenshare permission" }
        }
    }

    fun stopScreenSharing() {
        mediaManager.screenShare.disable(fromUser = true)
    }

    fun isAudioProcessingEnabled(): Boolean {
        return peerConnectionFactory.isAudioProcessingEnabled()
    }

    fun setAudioProcessingEnabled(enabled: Boolean) {
        return peerConnectionFactory.setAudioProcessingEnabled(enabled)
    }

    fun toggleAudioProcessing(): Boolean {
        return peerConnectionFactory.toggleAudioProcessing()
    }

    /** Disables all local capture devices. Used when leaving the call. */
    fun disableLocalCapture() {
        stopScreenSharing()
        mediaManager.camera.disable()
        mediaManager.microphone.disable()
    }

    fun cleanup() {
        mediaManager.cleanup()
    }
}
