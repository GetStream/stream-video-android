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

package io.getstream.video.android.core.call.connection

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.connection.coding.SelectiveVideoDecoderFactory
import io.getstream.video.android.core.call.utils.addAndConvertBuffers
import io.getstream.video.android.core.call.video.FilterVideoProcessor
import io.getstream.video.android.core.defaultAudioUsage
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.trace.PeerConnectionTraceKey
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.utils.safeCallWithDefault
import kotlinx.coroutines.CoroutineScope
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.Logging
import org.webrtc.ManagedAudioProcessingFactory
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ResolutionAdjustment
import org.webrtc.RtpCapabilities
import org.webrtc.SimulcastAlignedVideoEncoderFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.models.PublishOption
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds a factory that provides [PeerConnection]s when requested.
 *
 * @property context Used to build the underlying native components for the factory.
 * @property audioUsage signal to the system how the audio tracks are used.
 * @property audioProcessing Factory that provides audio processing capabilities.
 * Set this to [AudioAttributes.USAGE_MEDIA] if you want the audio track to behave like media, useful for livestreaming scenarios.
 * @property sharedEglBaseProvider Provider function that returns the EGL base context. This is lazy-evaluated to avoid
 * creating EglBase during construction, which is useful for unit testing.
 */
public class StreamPeerConnectionFactory(
    private val context: Context,
    @Deprecated("Use audioUsageProvider instead")
    private val audioUsage: Int = defaultAudioUsage,
    private val audioUsageProvider: (() -> Int) = { audioUsage },
    private var audioProcessing: ManagedAudioProcessingFactory? = null,
    private val audioBitrateProfileProvider: (() -> stream.video.sfu.models.AudioBitrateProfile)? = null,
    private val sharedEglBaseProvider: () -> EglBase = { EglBase.create() },
) {
    /**
     * The audio bitrate profile that was used when this factory was created.
     * This is captured when initAudioDeviceModule() is called and is used to detect
     * if the factory needs to be recreated when the profile changes during join
     */
    internal var audioBitrateProfile: stream.video.sfu.models.AudioBitrateProfile? = null

    private val webRtcLogger by taggedLogger("Call:WebRTC")
    private val audioLogger by taggedLogger("Call:AudioTrackCallback")

    private var audioSampleCallback: ((AudioSamples) -> Unit)? = null
    private var audioRecordDataCallback: (
        (audioFormat: Int, channelCount: Int, sampleRate: Int, sampleData: ByteBuffer) -> Unit
    )? = null

    // Provider function to get screen audio bytes from MediaManager on demand
    private var screenAudioBytesProvider: ((Int) -> ByteBuffer?)? = null

    // Provider function to check if microphone is enabled
    private var microphoneEnabledProvider: (() -> Boolean)? = null

    /**
     * Set to get callbacks when audio input from microphone is received.
     * This can be example used to detect whether a person is speaking
     * while muted.
     */
    public fun setAudioSampleCallback(callback: (AudioSamples) -> Unit) {
        audioSampleCallback = callback
    }

    /**
     * Set to get callbacks when audio input from microphone is received.
     * This can be example used to detect whether a person is speaking
     * while muted.
     */
    public fun setAudioRecordDataCallback(
        callback: (
            audioFormat: Int,
            channelCount: Int,
            sampleRate: Int,
            sampleData: ByteBuffer,
        ) -> Unit,
    ) {
        audioRecordDataCallback = callback
    }

    /**
     * Sets a provider function that returns screen audio bytes on demand.
     * The provider will be called with the number of bytes requested and should return
     * a ByteBuffer containing the requested bytes (may have fewer bytes if not enough data is available).
     * This should return null when screen sharing is not active.
     */
    internal fun setScreenAudioBytesProvider(provider: ((Int) -> ByteBuffer?)?) {
        screenAudioBytesProvider = provider
    }

    /**
     * Sets a provider function that returns whether the microphone is enabled.
     * This is used to determine if microphone audio should be included when mixing with screen audio.
     */
    internal fun setMicrophoneEnabledProvider(provider: (() -> Boolean)?) {
        microphoneEnabledProvider = provider
    }

    /**
     * Represents the EGL rendering context.
     * Todo : Remove this with the next major release
     */
    public val eglBase: EglBase by lazy {
        sharedEglBaseProvider()
    }

    /**
     * Default video decoder factory used to unpack video from the remote tracks.
     */
    private val videoDecoderFactory by lazy {
        SelectiveVideoDecoderFactory(sharedEglBaseProvider().eglBaseContext)
    }

    /**
     * Default encoder factory that supports Simulcast, used to send video tracks to the server.
     */
    private val videoEncoderFactory by lazy {
        SimulcastAlignedVideoEncoderFactory(
            sharedEglBaseProvider().eglBaseContext,
            true,
            true,
            ResolutionAdjustment.MULTIPLE_OF_16,
        )
    }

    /**
     * Factory that builds all the connections based on the extensive configuration provided under
     * the hood.
     */
    private val factory: PeerConnectionFactory by lazy { createFactory() }

    private var adm: JavaAudioDeviceModule? = null

    private fun createFactory(): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setInjectableLogger({ message, severity, label ->
                    when (severity) {
                        Logging.Severity.LS_VERBOSE -> {
                            // webRtcLogger.v { "[onLogMessage] label: $label, message: $message" }
                        }

                        Logging.Severity.LS_INFO -> {
                            webRtcLogger.i { "[onLogMessage] label: $label, message: $message" }
                        }

                        Logging.Severity.LS_WARNING -> {
                            webRtcLogger.w { "[onLogMessage] label: $label, message: $message" }
                        }

                        Logging.Severity.LS_ERROR -> {
                            webRtcLogger.e { "[onLogMessage] label: $label, message: $message" }
                        }

                        Logging.Severity.LS_NONE -> {
                            webRtcLogger.d { "[onLogMessage] label: $label, message: $message" }
                        }

                        else -> {}
                    }
                }, Logging.Severity.LS_VERBOSE)
                .createInitializationOptions(),
        )

        adm = initAudioDeviceModule()

        // Capture the audio bitrate profile when creating the factory
        val currentAudioBitrateProfile = audioBitrateProfileProvider?.invoke()
        val isMusicHighQuality = currentAudioBitrateProfile ==
            stream.video.sfu.models.AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY

        return PeerConnectionFactory.builder()
            .apply {
                // Disable audio processing (noise cancellation) when MUSIC_HIGH_QUALITY is enabled
                if (!isMusicHighQuality) {
                    audioProcessing?.also { setAudioProcessingFactory(it) }
                } else {
                    setAudioProcessingEnabled(false)
                }
            }
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setAudioDeviceModule(
                adm,
            )
            .createPeerConnectionFactory()
    }

    private fun initAudioDeviceModule(): JavaAudioDeviceModule? {
        // Capture the audio bitrate profile when initializing the audio device module
        audioBitrateProfile = audioBitrateProfileProvider?.invoke()

        val isMusicHighQuality = audioBitrateProfile ==
            stream.video.sfu.models.AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY
        val useHardwareAcousticEchoCanceler = if (isMusicHighQuality) {
            false
        } else {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
        val useHardwareNoiseSuppressor = if (isMusicHighQuality) {
            false
        } else {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }

        adm = JavaAudioDeviceModule
            .builder(context)
            .setUseHardwareAcousticEchoCanceler(useHardwareAcousticEchoCanceler)
            .apply {
                if (audioUsageProvider.invoke() != defaultAudioUsage) {
                    setAudioAttributes(
                        AudioAttributes.Builder().setUsage(audioUsageProvider.invoke())
                            .build(),
                    )
                }
            }
            .setUseHardwareNoiseSuppressor(useHardwareNoiseSuppressor)
            .setAudioRecordErrorCallback(object :
                JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(p0: String?) {
                    audioLogger.w { "[onWebRtcAudioRecordInitError] $p0" }
                }

                override fun onWebRtcAudioRecordStartError(
                    p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                    p1: String?,
                ) {
                    audioLogger.w { "[onWebRtcAudioRecordInitError] $p1" }
                }

                override fun onWebRtcAudioRecordError(p0: String?) {
                    audioLogger.w { "[onWebRtcAudioRecordError] $p0" }
                }
            })
            .setAudioTrackErrorCallback(object :
                JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(p0: String?) {
                    audioLogger.w { "[onWebRtcAudioTrackInitError] $p0" }
                }

                override fun onWebRtcAudioTrackStartError(
                    p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                    p1: String?,
                ) {
                    audioLogger.w { "[onWebRtcAudioTrackStartError] $p0" }
                }

                override fun onWebRtcAudioTrackError(p0: String?) {
                    audioLogger.w { "[onWebRtcAudioTrackError] $p0" }
                }
            })
            .setAudioRecordStateCallback(object :
                JavaAudioDeviceModule.AudioRecordStateCallback {
                override fun onWebRtcAudioRecordStart() {
                    audioLogger.d { "[onWebRtcAudioRecordStart] no args" }
                }

                override fun onWebRtcAudioRecordStop() {
                    audioLogger.d { "[onWebRtcAudioRecordStop] no args" }
                }
            })
            .setAudioTrackStateCallback(object :
                JavaAudioDeviceModule.AudioTrackStateCallback {
                override fun onWebRtcAudioTrackStart() {
                    audioLogger.d { "[onWebRtcAudioTrackStart] no args" }
                }

                override fun onWebRtcAudioTrackStop() {
                    audioLogger.d { "[onWebRtcAudioTrackStop] no args" }
                }
            })
            .setSamplesReadyCallback {
                audioSampleCallback?.invoke(it)
            }
            .setAudioBufferCallback { audioBuffer, audioFormat, channelCount, sampleRate, bytesRead, captureTimeNs ->
                audioRecordDataCallback?.invoke(
                    audioFormat,
                    channelCount,
                    sampleRate,
                    audioBuffer,
                )

                if (bytesRead > 0) {
                    mixAudioBuffers(bytesRead, audioBuffer)
                }

                captureTimeNs
            }
            .setUseStereoOutput(true)
            .createAudioDeviceModule().also {
                it.setMicrophoneMute(false)
                it.setSpeakerMute(false)
            }

        return adm
    }

    /**
     * Mixes screen share audio with microphone audio before sending to the peer connection.
     *
     *  When screen sharing with audio is active, it retrieves screen audio bytes and mixes
     *  them with the microphone audio (or silence if the microphone is disabled) to create a
     *  combined audio stream.
     *
     * The mixing process:
     * 1. Retrieves screen audio bytes from the [screenAudioBytesProvider] if available
     * 2. Converts both audio buffers from ByteBuffer to ShortArray (PCM 16-bit format)
     * 3. If microphone is enabled: mixes microphone audio with screen audio, if any
     * 4. If microphone is disabled: mixes silent microphone samples with screen audio, if any
     * 5. Writes the mixed audio back into the [audioBuffer] for transmission
     *
     * @param bytesRead The number of bytes read from the microphone audio buffer
     * @param audioBuffer The microphone audio buffer that will be modified in-place with the mixed audio.
     *                    Expected to be in PCM 16-bit little-endian format.
     */
    internal fun mixAudioBuffers(bytesRead: Int, audioBuffer: ByteBuffer) {
        // Request screen audio bytes from MediaManager on demand
        // Returns null if screen share audio is not enabled
        val screenAudioBuffer = screenAudioBytesProvider?.invoke(bytesRead)

        if (screenAudioBuffer != null && screenAudioBuffer.remaining() > 0) {
            screenAudioBuffer.position(0)
            audioBuffer.position(0)

            // Convert screen audio (ByteBuffer) to ShortArray
            val screenSamples = ShortArray(screenAudioBuffer.limit() / 2)
            screenAudioBuffer.order(ByteOrder.LITTLE_ENDIAN)
            screenAudioBuffer.asShortBuffer()[screenSamples]

            val isMicrophoneEnabled = microphoneEnabledProvider?.invoke() ?: true
            val mixedAudio = if (isMicrophoneEnabled) {
                // Convert microphone audio (ByteBuffer) to ShortArray
                val micSamples = ShortArray(audioBuffer.limit() / 2)
                audioBuffer.order(ByteOrder.LITTLE_ENDIAN)
                audioBuffer.asShortBuffer()[micSamples]

                // Mix the audio buffers
                addAndConvertBuffers(
                    micSamples,
                    micSamples.size,
                    screenSamples,
                    screenSamples.size,
                )
            } else {
                // Microphone is disabled, only send screen audio
                // Create silent microphone samples (all zeros) and mix with screen audio
                val silentMicSamples = ShortArray(audioBuffer.limit() / 2) { 0 }
                addAndConvertBuffers(
                    silentMicSamples,
                    silentMicSamples.size,
                    screenSamples,
                    screenSamples.size,
                )
            }

            // Put the mixed audio back into the buffer
            audioBuffer.clear()
            audioBuffer.put(mixedAudio)
        }
    }

    /**
     * Returns the capabilities of the sender based on the [mediaType].
     *
     * @param mediaType The type of media we're sending.
     */
    fun getSenderCapabilities(mediaType: MediaStreamTrack.MediaType): RtpCapabilities {
        return factory.getRtpSenderCapabilities(mediaType)
    }

    /**
     * Builds a [StreamPeerConnection] that wraps the WebRTC [PeerConnection] and exposes several
     * helpful handlers.
     *
     * @param coroutineScope Scope used for asynchronous operations.
     * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
     * @param type The type of connection, either a subscriber of a publisher.
     * @param mediaConstraints Constraints used for audio and video tracks in the connection.
     * @param onStreamAdded Handler when a new [MediaStream] gets added.
     * @param onNegotiationNeeded Handler when there's a new negotiation.
     * @param onIceCandidateRequest Handler whenever we receive [IceCandidate]s.
     * @return [StreamPeerConnection] That's fully set up and can be observed and used to send and
     * receive tracks.
     */
    public fun makePeerConnection(
        configuration: PeerConnection.RTCConfiguration,
        type: StreamPeerType,
        mediaConstraints: MediaConstraints,
        onStreamAdded: ((MediaStream) -> Unit)? = null,
        onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)? = null,
        onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)? = null,
        maxPublishingBitrate: Int = 1_200_000,
        debugText: String = "",
    ): StreamPeerConnection {
        val peerConnection = StreamPeerConnection(
            type = type,
            mediaConstraints = mediaConstraints,
            onStreamAdded = onStreamAdded,
            onNegotiationNeeded = onNegotiationNeeded,
            onIceCandidate = onIceCandidateRequest,
            maxBitRate = maxPublishingBitrate,
            onRejoinNeeded = { },
            tracer = Tracer(type.toPeerType().name),
            tag = debugText,
            onFastReconnectNeeded = {},
        )
        val connection = makePeerConnectionInternal(
            configuration = configuration,
            observer = peerConnection,
        )
        webRtcLogger.d { "type $type $peerConnection is now monitoring $connection" }
        peerConnection.initialize(connection)

        return peerConnection
    }

    internal fun makeSubscriber(
        coroutineScope: CoroutineScope,
        sessionId: String,
        sfuClient: SignalServerService,
        configuration: PeerConnection.RTCConfiguration,
        enableStereo: Boolean = true,
        tracer: Tracer,
        onIceCandidateRequest: (IceCandidate, StreamPeerType) -> Unit,
        rejoin: () -> Unit,
        fastReconnect: () -> Unit,
        sfuConnectionModule: SfuConnectionModule,
    ): Subscriber {
        val peerConnection = Subscriber(
            sessionId = sessionId,
            sfuClient = sfuClient,
            coroutineScope = coroutineScope,
            enableStereo = enableStereo,
            tracer = tracer,
            rejoin = rejoin,
            fastReconnect = fastReconnect,
            sfuConnectionModule = sfuConnectionModule,
            onIceCandidateRequest = onIceCandidateRequest,
        )
        val connection = makePeerConnectionInternal(
            configuration = configuration,
            observer = peerConnection,
        )
        webRtcLogger.d { "type $peerConnection is now monitoring $connection" }
        peerConnection.initialize(connection)
        peerConnection.addTransceivers()

        val traceData = safeCallWithDefault(null) {
            "iceServers=${
                configuration.iceServers.joinToString {
                    it.toString()
                }
            } , budlePolicy=${configuration.bundlePolicy}, sdpSemantics=${configuration.sdpSemantics}"
        }
        peerConnection.tracer().trace(PeerConnectionTraceKey.CREATE.value, traceData)
        return peerConnection
    }

    internal fun makePublisher(
        mediaManager: MediaManagerImpl,
        publishOptions: List<PublishOption>,
        coroutineScope: CoroutineScope,
        configuration: PeerConnection.RTCConfiguration,
        mediaConstraints: MediaConstraints,
        onStreamAdded: ((MediaStream) -> Unit)? = null,
        onNegotiationNeeded: (StreamPeerConnection, StreamPeerType) -> Unit,
        onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)? = null,
        maxPublishingBitrate: Int = 1_200_000,
        sfuClient: SignalServerService,
        sessionId: String,
        tracer: Tracer,
        rejoin: () -> Unit = {},
        fastReconnect: () -> Unit = {},
        isHifiAudioEnabled: Boolean = false,
    ): Publisher {
        val peerConnection = Publisher(
            sessionId = sessionId,
            sfuClient = sfuClient,
            peerConnectionFactory = this,
            mediaManager = mediaManager,
            publishOptions = publishOptions,
            coroutineScope = coroutineScope,
            type = StreamPeerType.PUBLISHER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = onStreamAdded,
            onNegotiationNeeded = onNegotiationNeeded,
            onIceCandidate = onIceCandidate,
            maxBitRate = maxPublishingBitrate,
            tracer = tracer,
            rejoin = rejoin,
            fastReconnect = fastReconnect,
            isHifiAudioEnabled = isHifiAudioEnabled,
        )
        val connection = makePeerConnectionInternal(
            configuration = configuration,
            observer = peerConnection,
        )
        webRtcLogger.d { "type ${StreamPeerType.PUBLISHER} $peerConnection is now monitoring $connection" }
        peerConnection.initialize(connection)
        val traceData = safeCallWithDefault(null) {
            "iceServers=${
                configuration.iceServers.joinToString {
                    it.toString()
                }
            } , budlePolicy=${configuration.bundlePolicy}, sdpSemantics=${configuration.sdpSemantics}"
        }
        peerConnection.tracer().trace(PeerConnectionTraceKey.CREATE.value, traceData)

        return peerConnection
    }

    /**
     * Builds a [PeerConnection] internally that connects to the server and is able to send and
     * receive tracks.
     *
     * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
     * @param observer Handler used to observe different states of the connection.
     * @return [PeerConnection] that's fully set up.
     */
    internal fun makePeerConnectionInternal(
        configuration: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer?,
    ): PeerConnection {
        return requireNotNull(
            factory.createPeerConnection(
                configuration,
                observer,
            ),
        )
    }

    /**
     * Builds a [VideoSource] from the [factory] that can be used for regular video share (camera)
     * or screen sharing.
     *
     * @param isScreencast If we're screen sharing using this source.
     * @return [VideoSource] that can be used to build tracks.
     */

    internal fun makeVideoSource(
        isScreencast: Boolean,
        filterVideoProcessor: FilterVideoProcessor,
    ): VideoSource =
        factory.createVideoSource(isScreencast).apply {
            setVideoProcessor(filterVideoProcessor)
        }

    /**
     * Builds a [VideoTrack] from the [factory] that can be used for regular video share (camera)
     * or screen sharing.
     *
     * @param source The [VideoSource] used for the track.
     * @param trackId The unique ID for this track.
     * @return [VideoTrack] That represents a video feed.
     */
    public fun makeVideoTrack(
        source: VideoSource,
        trackId: String,
    ): VideoTrack = factory.createVideoTrack(trackId, source)

    /**
     * Builds an [AudioSource] from the [factory] that can be used for audio sharing.
     *
     * @param constraints The constraints used to change the way the audio behaves.
     * @return [AudioSource] that can be used to build tracks.
     */
    public fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
        factory.createAudioSource(constraints)

    /**
     * Builds an [AudioTrack] from the [factory] that can be used for regular video share (camera)
     * or screen sharing.
     *
     * @param source The [AudioSource] used for the track.
     * @param trackId The unique ID for this track.
     * @return [AudioTrack] That represents an audio feed.
     */
    public fun makeAudioTrack(
        source: AudioSource,
        trackId: String,
    ): AudioTrack = factory.createAudioTrack(trackId, source)

    /**
     * True if the audio processing is enabled, false otherwise.
     */
    public fun isAudioProcessingEnabled(): Boolean {
        return audioProcessing?.isEnabled ?: false
    }

    /**
     * Sets the audio processing on or off.
     */
    public fun setAudioProcessingEnabled(enabled: Boolean) {
        audioProcessing?.isEnabled = enabled
    }

    /**
     * Toggles the audio processing on and off.
     */
    public fun toggleAudioProcessing(): Boolean {
        return audioProcessing?.let {
            it.isEnabled = !it.isEnabled
            it.isEnabled
        } ?: false
    }

    /**
     * Updates the audio track usage for the audio device module.
     * This allows toggling between different audio usage types (e.g., USAGE_MEDIA vs USAGE_VOICE_COMMUNICATION).
     *
     * @param audioUsage The audio usage value to set (e.g., AudioAttributes.USAGE_MEDIA or AudioAttributes.USAGE_VOICE_COMMUNICATION)
     * @return true if the update was successful, false if the ADM is not available or the update failed
     */
    internal fun updateAudioTrackUsage(audioUsage: Int): Boolean {
        return adm?.updateAudioTrackUsage(audioUsage) ?: false
    }

    /**
     * Disposes the factory and releases resources.
     * This should only be called when no active peer connections are using it.
     */
    internal fun dispose() {
        try {
            factory.dispose()
        } catch (e: Exception) {
            webRtcLogger.w { "Error disposing factory: ${e.message}" }
        }
        adm?.release()
        adm = null
    }
}
