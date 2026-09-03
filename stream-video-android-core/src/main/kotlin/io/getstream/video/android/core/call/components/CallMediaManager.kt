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

    /**
     * Audio-processing state this call wants, remembered separately from the factory so it
     * survives one not existing yet or being recreated. Applying it must never itself build a
     * factory: one created before join captures the pre-join audio bitrate profile and defeats
     * [ensureFactoryMatchesAudioProfile].
     *
     * The processor itself is owned by the client and shared by every call, and its enabled flag
     * lives on that one instance. Rather than clearing the flag when a call ends — which reaches
     * across into whatever call runs next — every factory applies the state its own call asked
     * for as it is built. Defaults to off so a call that never asks does not inherit.
     */
    private var desiredAudioProcessingEnabled: Boolean = false

    /**
     * Platform noise-suppressor state this call asked for, or null while the builder default
     * stands. Kept alongside [desiredAudioProcessingEnabled] and for the same reason: the wanted
     * state has to outlive the factory so a recreation cannot silently drop it.
     */
    private var desiredHardwareNoiseSuppressorEnabled: Boolean? = null

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
                ).also { factory ->
                    factory.setAudioProcessingEnabled(desiredAudioProcessingEnabled)
                    desiredHardwareNoiseSuppressorEnabled?.let {
                        factory.setHardwareNoiseSuppressorEnabled(it)
                    }
                }
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
        val previous = _peerConnectionFactory
        // Next access to peerConnectionFactory will recreate it with current profile
        _peerConnectionFactory = null
        if (previous?.hasAudioProcessingAttached() == true) {
            // The shared audio processor is torn down when the native factory holding it is
            // released, which would leave the client without one for every later call. Dropping
            // the factory instead leaks it, the same way a factory is dropped on call cleanup.
            logger.i { "Keeping the previous factory alive: it holds the shared audio processor" }
            return
        }
        previous?.dispose()
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

    /**
     * Whether noise cancellation is on for this call: what is actually processing once a factory
     * exists, and what the call asked for before then.
     *
     * Never builds a factory. Once one exists its answer wins, so a call under MUSIC_HIGH_QUALITY
     * reports off even though it asked for on — nothing is attached to process its audio.
     */
    fun isAudioProcessingEnabled(): Boolean = if (_peerConnectionFactory != null) {
        isAudioProcessingEnabledIfCreated()
    } else {
        desiredAudioProcessingEnabled
    }

    /**
     * Audio-processing state as far as it is known, without forcing the peer-connection factory
     * to be built. False before the factory exists — nothing can be processing yet.
     *
     * Used where the state is reported rather than changed — state publishing, the policy gate
     * and signalling — which must never bring a factory into existence: one created before join
     * would be built with the pre-join audio bitrate profile and defeat
     * [ensureFactoryMatchesAudioProfile].
     */
    fun isAudioProcessingEnabledIfCreated(): Boolean =
        _peerConnectionFactory?.isAudioProcessingEnabled() ?: false

    /**
     * Whether there is a noise-cancellation processor wired into this call's native factory at all.
     *
     * Distinguishes "the processor refused to change" from "there is no processor" — without it
     * a call configured with no [org.webrtc.ManagedAudioProcessingFactory] looks like a failure
     * every time noise cancellation is asked for. Never builds a factory, for the reason given on
     * [isAudioProcessingEnabledIfCreated].
     */
    fun isAudioProcessingReachable(): Boolean =
        _peerConnectionFactory?.hasAudioProcessingAttached() ?: false

    /**
     * Whether this call wants audio processing, whether or not a factory exists to run it yet.
     *
     * The wanted state outlives the factory, so a policy that withholds noise cancellation has to
     * clear it — otherwise the next factory applies it after the server said no.
     */
    fun isAudioProcessingWanted(): Boolean = desiredAudioProcessingEnabled

    /** Forgets the wanted audio-processing state, so nothing is re-applied after the call ends. */
    fun resetDesiredAudioProcessing() {
        desiredAudioProcessingEnabled = false
    }

    /**
     * Records the wanted audio-processing state and applies it to the factory if one exists.
     * A factory built later picks the value up on creation, so this never has to build one.
     */
    fun setAudioProcessingEnabled(enabled: Boolean) {
        desiredAudioProcessingEnabled = enabled
        _peerConnectionFactory?.setAudioProcessingEnabled(enabled)
    }

    /**
     * Flips the wanted audio-processing state, applying it if a factory exists.
     *
     * Goes through [setAudioProcessingEnabled] rather than the factory so a toggle before joining
     * cannot build one: a factory created there captures the pre-join audio bitrate profile, and
     * one holding the shared processor is kept rather than released, so the profile would be
     * pinned for the rest of the call.
     */
    fun toggleAudioProcessing(): Boolean {
        setAudioProcessingEnabled(!desiredAudioProcessingEnabled)
        return isAudioProcessingEnabled()
    }

    /**
     * Records the wanted platform noise-suppressor state and applies it if a factory exists.
     *
     * Never builds one: like [setAudioProcessingEnabled], a factory created here would capture the
     * pre-join audio bitrate profile. A factory built later picks the value up on creation.
     *
     * @return true when the running capture session accepted the change.
     */
    fun setHardwareNoiseSuppressorEnabled(enabled: Boolean): Boolean {
        desiredHardwareNoiseSuppressorEnabled = enabled
        return _peerConnectionFactory?.setHardwareNoiseSuppressorEnabled(enabled) ?: false
    }

    /** Forgets the wanted noise-suppressor state, so nothing is re-applied after the call ends. */
    fun resetDesiredHardwareNoiseSuppressor() {
        desiredHardwareNoiseSuppressorEnabled = null
    }

    /** Disables all local capture devices. Used when leaving the call. */
    fun disableLocalCapture() {
        stopScreenSharing()
        mediaManager.camera.disable()
        mediaManager.microphone.disable()
    }

    fun cleanup() {
        // The wanted state must not outlive the call: a reused Call would otherwise re-apply it
        // to the factory built for the next session.
        resetDesiredAudioProcessing()
        resetDesiredHardwareNoiseSuppressor()
        mediaManager.cleanup()
    }
}
