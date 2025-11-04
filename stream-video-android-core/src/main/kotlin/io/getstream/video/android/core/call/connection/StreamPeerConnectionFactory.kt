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
import io.getstream.video.android.core.call.video.FilterVideoProcessor
import io.getstream.video.android.core.defaultAudioUsage
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

/**
 * Builds a factory that provides [PeerConnection]s when requested.
 *
 * @property context Used to build the underlying native components for the factory.
 * @property audioUsage signal to the system how the audio tracks are used.
 * @property audioProcessing Factory that provides audio processing capabilities.
 * Set this to [AudioAttributes.USAGE_MEDIA] if you want the audio track to behave like media, useful for livestreaming scenarios.
 * @property sharedEglBase Optional EGL base context. If not provided, will create its own.
 */
public class StreamPeerConnectionFactory(
    private val context: Context,
    @Deprecated("Use audioUsageProvider instead")
    private val audioUsage: Int = defaultAudioUsage,
    private val audioUsageProvider: (() -> Int) = { audioUsage },
    private var audioProcessing: ManagedAudioProcessingFactory? = null,
    private val audioBitrateProfileProvider: (() -> stream.video.sfu.models.AudioBitrateProfile)? = null,
    private val eglBase: EglBase,
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
     * Default video decoder factory used to unpack video from the remote tracks.
     */
    private val videoDecoderFactory by lazy {
        SelectiveVideoDecoderFactory(eglBase.eglBaseContext)
    }

    /**
     * Default encoder factory that supports Simulcast, used to send video tracks to the server.
     */
    private val videoEncoderFactory by lazy {
        SimulcastAlignedVideoEncoderFactory(
            eglBase.eglBaseContext,
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

    var adm: JavaAudioDeviceModule? = null

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

        return PeerConnectionFactory.builder()
            .apply {
                audioProcessing?.also { setAudioProcessingFactory(it) }
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
            .setAudioRecordDataCallback { audioFormat, channelCount, sampleRate, audioBuffer ->
                audioRecordDataCallback?.invoke(
                    audioFormat,
                    channelCount,
                    sampleRate,
                    audioBuffer,
                )
            }
            .setUseStereoOutput(true)
            .createAudioDeviceModule().also {
                it.setMicrophoneMute(false)
                it.setSpeakerMute(false)
            }

        return adm
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
    ): Subscriber {
        val peerConnection = Subscriber(
            sessionId = sessionId,
            sfuClient = sfuClient,
            coroutineScope = coroutineScope,
            tracer = tracer,
            enableStereo = enableStereo,
            rejoin = rejoin,
            fastReconnect = fastReconnect,
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
     * Disposes the factory and releases resources.
     * This should only be called when no active peer connections are using it.
     */
    public fun dispose() {
        try {
            factory.dispose()
        } catch (e: Exception) {
            webRtcLogger.w { "Error disposing factory: ${e.message}" }
        }
        adm?.release()
        adm = null
    }
}
