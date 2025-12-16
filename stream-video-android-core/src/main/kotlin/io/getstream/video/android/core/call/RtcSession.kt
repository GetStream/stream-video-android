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

package io.getstream.video.android.core.call

import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.THERMAL_STATUS_CRITICAL
import android.os.PowerManager.THERMAL_STATUS_EMERGENCY
import android.os.PowerManager.THERMAL_STATUS_LIGHT
import android.os.PowerManager.THERMAL_STATUS_MODERATE
import android.os.PowerManager.THERMAL_STATUS_NONE
import android.os.PowerManager.THERMAL_STATUS_SEVERE
import android.os.PowerManager.THERMAL_STATUS_SHUTDOWN
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.extractCause
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallStatsReport
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.RtcException
import io.getstream.video.android.core.events.CallEndedSfuEvent
import io.getstream.video.android.core.events.ChangePublishOptionsEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.ICERestartEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.InboundStateNotificationEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SfuDataRequest
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.core.toJson
import io.getstream.video.android.core.trace.PeerConnectionTraceKey
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trace.TracerManager
import io.getstream.video.android.core.trace.serialize
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.SerialProcessor
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.defaultConstraints
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.IOException
import org.json.JSONArray
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.SessionDescription
import retrofit2.HttpException
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.LeaveCallRequest
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.event.SfuRequest
import stream.video.sfu.models.AndroidState
import stream.video.sfu.models.AndroidThermalState
import stream.video.sfu.models.ClientDetails
import stream.video.sfu.models.Device
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.models.OS
import stream.video.sfu.models.ParticipantSource
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.Sdk
import stream.video.sfu.models.SdkType
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.WebsocketReconnectStrategy
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.ICERestartResponse
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.Reconnection
import stream.video.sfu.signal.SendStatsRequest
import stream.video.sfu.signal.Telemetry
import stream.video.sfu.signal.TrackMuteState
import stream.video.sfu.signal.TrackSubscriptionDetails
import stream.video.sfu.signal.UpdateMuteStatesRequest
import stream.video.sfu.signal.UpdateMuteStatesResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import java.util.Collections

/**
 * Keeps track of which track is being rendered at what resolution.
 * Also stores if the track is visible or not
 */
data class TrackDimensions(
    var dimensions: VideoDimension,
    var visible: Boolean = false,
)

/**
 * The RtcSession sets up 2 peer connection
 * - The publisher peer connection
 * - The subscriber peer connection
 *
 * It handles everything webrtc related.
 * State is handled by the call state class
 *
 * @see CallState
 *
 * Audio/video management is done by the MediaManager
 *
 * @see MediaManagerImpl
 *
 * This is how the offer/answer cycle works
 *
 * * sessionId is created locally as a random UUID
 * * create the peer connections
 * * capture audio and video (if we're not doing so already, in many apps it should already be on for the preview screen)
 * * execute the join request
 * * add the audio/video tracks which triggers onNegotiationNeeded
 * * onNegotiationNeeded(which calls SetPublisherRequest)
 * * JoinCallResponseEvent returns info on the call's state
 *
 * Dynascale automatically negotiates resolutions across clients
 *
 * * We send what resolutions we want using UpdateSubscriptionsRequest.
 * * It should be triggered as we paginate through participants
 * * Or when the UI layout changes
 * * The SFU tells us what resolution to publish using the ChangePublishQualityEvent event
 *
 * For developers: RtcSession throws [IllegalStateException] because its [coroutineScope] & [rtcSessionScope] throws it
 */
public class RtcSession internal constructor(
    client: StreamVideo,
    private val sessionCounter: Int = 0,
    private val powerManager: PowerManager?,
    private val call: Call,
    private val sessionId: String,
    private val apiKey: String,
    private val lifecycle: Lifecycle,
    internal var sfuUrl: String,
    internal var sfuWsUrl: String,
    internal var sfuToken: String,
    internal var remoteIceServers: List<IceServer>,
    internal val clientImpl: StreamVideoClient = client as StreamVideoClient,
    private val supervisorJob: CompletableJob = SupervisorJob(),
    private val scopeProvider: ScopeProvider = call.scopeProvider,
    private val coroutineScope: CoroutineScope = scopeProvider.getCoroutineScope(supervisorJob),
    private val rtcSessionScope: CoroutineScope = scopeProvider.getRtcSessionScope(
        supervisorJob,
        call.id,
    ),
    private val serialProcessor: SerialProcessor = SerialProcessor(rtcSessionScope),
    private val tracerManager: TracerManager = TracerManager(clientImpl.enableStatsCollection),
    private val sfuTracer: Tracer = tracerManager.tracer(
        "${sessionCounter + 1}-${
            sfuUrl.replace(
                "https://",
                "",
            ).replace("/twirp", "")
        }",
    ),
    private val sfuConnectionModuleProvider: () -> SfuConnectionModule = {
        SfuConnectionModule(
            context = clientImpl.context,
            apiKey = apiKey,
            apiUrl = sfuUrl,
            wssUrl = sfuWsUrl,
            connectionTimeoutInMs = 2000L,
            lifecycle = lifecycle,
            onSignalingLost = { error ->
                call.debug.fastReconnect()
            },
            tracer = sfuTracer,
            tokenRepository = TokenRepository(sfuToken),
        )
    },
) {
    private var muteStateSyncJob: Job? = null
    private var subscriberListenJob: Job? = null
    private val oneBasedSessionCounter = sessionCounter + 1

    private var stateJob: Job? = null
    private var errorJob: Job? = null
    private var eventJob: Job? = null
    internal val socket
        get() = sfuConnectionModule.socketConnection

    private val publisherTracer = tracerManager.tracer("$oneBasedSessionCounter-pub")
    private val subscriberTracer = tracerManager.tracer("$oneBasedSessionCounter-sub")

    private val logger by taggedLogger("Video:RtcSession")
    private val parser: VideoParser = MoshiVideoParser()

    /**
     * Data class representing a track that arrived before its participant existed.
     * Based on the JS SDK's orphaned track pattern.
     *
     * These tracks are stored temporarily until the participant information arrives,
     * at which point they can be reconciled and attached to the participant.
     */
    private data class OrphanedTrack(
        val sessionId: String,
        val trackType: TrackType,
        val track: MediaTrack,
    )

    /**
     * Storage for tracks that arrived before their participants.
     * Synchronized list to handle concurrent access from different coroutines.
     */
    private val orphanedTracks = mutableListOf<OrphanedTrack>()

    internal val _peerConnectionStates =
        MutableStateFlow<Pair<PeerConnection.PeerConnectionState?, PeerConnection.PeerConnectionState?>?>(
            null,
        )

    internal val trackOverridesHandler = TrackOverridesHandler(
        onOverridesUpdate = {
            setVideoSubscriptions()
            call.state._participantVideoEnabledOverrides.value = it.mapValues { it.value.visible }
        },
        logger = logger,
    )

    private fun getTrack(sessionId: String, type: TrackType): MediaTrack? {
        val track = subscriber?.getTrack(
            sessionId,
            type,
        )
        if (track == null) {
            logger.w { "[getTrack] #sfu; #track; track is null for sessionId: $sessionId, type: $type" }
        }
        return track
    }

    private fun setTrack(sessionId: String, type: TrackType, track: MediaTrack) {
        val participant = call.state.getParticipantBySessionId(sessionId)

        if (participant == null) {
            logger.w {
                "[setTrack] #orphaned-track; Participant not found for sessionId=$sessionId, " +
                    "registering orphaned track (type=$type)"
            }
            registerOrphanedTrack(sessionId, type, track)
            return
        }

        when (type) {
            TrackType.TRACK_TYPE_VIDEO -> {
                participant.setVideoTrack(track.asVideoTrack())
            }

            TrackType.TRACK_TYPE_AUDIO -> {
                participant._audioTrack.value = track.asAudioTrack()
            }

            TrackType.TRACK_TYPE_SCREEN_SHARE, TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO -> {
                participant._screenSharingTrack.value = track.asVideoTrack()
            }

            TrackType.TRACK_TYPE_UNSPECIFIED -> {
                logger.w { "Unspecified track type" }
            }
        }
    }

    /**
     * Registers a track that arrived before its participant existed.
     * The track will be stored until the participant is created, at which point
     * it can be reconciled and attached via [takeOrphanedTracks].
     *
     * Based on the JS SDK's orphaned track pattern.
     */
    private fun registerOrphanedTrack(sessionId: String, trackType: TrackType, track: MediaTrack) {
        synchronized(orphanedTracks) {
            orphanedTracks.add(OrphanedTrack(sessionId, trackType, track))
            logger.i {
                "[registerOrphanedTrack] #orphaned-track; Registered track for sessionId=$sessionId, " +
                    "type=$trackType, total orphaned=${orphanedTracks.size}"
            }
        }
    }

    /**
     * Retrieves and removes all orphaned tracks for a specific sessionId.
     * Returns a list of track type to track pairs that can be attached to the participant.
     *
     * Based on the JS SDK's takeOrphanedTracks pattern.
     */
    private fun takeOrphanedTracks(sessionId: String): List<Pair<TrackType, MediaTrack>> {
        return synchronized(orphanedTracks) {
            val tracks = orphanedTracks.filter { it.sessionId == sessionId }
            if (tracks.isNotEmpty()) {
                orphanedTracks.removeAll(tracks)
                logger.i {
                    "[takeOrphanedTracks] #orphaned-track; Retrieved ${tracks.size} orphaned tracks " +
                        "for sessionId=$sessionId, remaining orphaned=${orphanedTracks.size}"
                }
            }
            tracks.map { it.trackType to it.track }
        }
    }

    /**
     * Reconciles orphaned tracks for a participant by attaching any tracks
     * that arrived before the participant was created.
     *
     * Based on the JS SDK's reconcileOrphanedTracks pattern.
     */
    private fun reconcileOrphanedTracks(sessionId: String) {
        val tracks = takeOrphanedTracks(sessionId)
        if (tracks.isEmpty()) return

        logger.i {
            "[reconcileOrphanedTracks] #orphaned-track; Reconciling ${tracks.size} orphaned tracks " +
                "for sessionId=$sessionId"
        }

        tracks.forEach { (trackType, track) ->
            setTrack(sessionId, trackType, track)
        }
    }

    private fun getLocalTrack(type: TrackType): MediaTrack? {
        return getTrack(sessionId, type)
    }

    private fun setLocalTrack(type: TrackType, track: MediaTrack) {
        subscriber?.setTrack(sessionId, type, track)
        return setTrack(sessionId, type, track)
    }

    /**
     * Creates and publishes an audio track for transmitting audio.
     * This is used both when microphone is enabled and when screen sharing starts with muted microphone.
     */
    private suspend fun createAndPublishAudioTrack() {
        val canUserSendAudio = call.state.ownCapabilities.value.contains(
            OwnCapability.SendAudio,
        )
        if (!canUserSendAudio) {
            return
        }

        setMuteState(isEnabled = true, TrackType.TRACK_TYPE_AUDIO)
        val streamId = buildTrackId(TrackType.TRACK_TYPE_AUDIO)
        val track = publisher?.publishStream(
            streamId,
            TrackType.TRACK_TYPE_AUDIO,
        )

        setLocalTrack(
            TrackType.TRACK_TYPE_AUDIO,
            AudioTrack(
                streamId = streamId,
                audio = track as org.webrtc.AudioTrack,
            ),
        )
    }

    /**
     * Connection and WebRTC.
     */

    private var iceServers = buildRemoteIceServers(remoteIceServers)

    private val connectionConfiguration: PeerConnection.RTCConfiguration
        get() = buildConnectionConfiguration(iceServers)

    internal var subscriber: Subscriber? = null
    internal var publisher: Publisher? = null

    internal lateinit var sfuConnectionModule: SfuConnectionModule

    private val clientDetails = ClientDetails(
        os = OS(
            name = "Android",
            version = Build.VERSION.SDK_INT.toString(),
        ),
        device = Device(
            name = "${Build.MANUFACTURER} : ${Build.MODEL}",
        ),
        sdk = Sdk(
            type = SdkType.SDK_TYPE_ANDROID,
            major = BuildConfig.STREAM_VIDEO_VERSION_MAJOR.toString(),
            minor = BuildConfig.STREAM_VIDEO_VERSION_MINOR.toString(),
            patch = BuildConfig.STREAM_VIDEO_VERSION_PATCH.toString(),
        ),
    )

    /**
     * Used during a SFU migration as a temporary new SFU connection. Is null before and after
     * the migration is finished.
     */
    private var sfuConnectionMigrationModule: SfuConnectionModule? = null

    private val _sfuSfuSocketState =
        MutableStateFlow<SfuSocketState>(SfuSocketState.Disconnected.Stopped)
    val sfuSocketState = _sfuSfuSocketState.asStateFlow()

    /**
     * Check if the SDK is initialized.
     */
    internal fun isSDKInitialized() = StreamVideo.isInstalled

    init {
        if (!isSDKInitialized()) {
            throw IllegalArgumentException(
                "SDK hasn't been initialised yet - can't start a RtcSession",
            )
        }
        logger.i { "<init> #sfu; #track; no args" }

        // step 1 setup the peer connections
        // publisher = createPublisher()

        listenToSubscriberConnection()
        val sfuConnectionModule: SfuConnectionModule = sfuConnectionModuleProvider.invoke()
        setSfuConnectionModule(sfuConnectionModule)

        subscriber = createSubscriber()

        coroutineScope.launch {
            subscriber?.streams()?.collect {
                val (sessionId, trackType, track) = it
                logger.d {
                    "[streams] #sfu; #track; sessionId: $sessionId, trackType: $trackType, mediaStream: $track"
                }
                setTrack(sessionId, trackType, track)
            }
        }
        listenToSfuSocket()

        call.peerConnectionFactory.setAudioSampleCallback { it ->
            call.processAudioSample(it)
        }

        call.peerConnectionFactory.setAudioRecordDataCallback { audioFormat, channelCount, sampleRate, sampleData ->
            call.audioFilter?.applyFilter(
                audioFormat = audioFormat,
                channelCount = channelCount,
                sampleRate = sampleRate,
                sampleData = sampleData,
            )
        }

        // Set up screen audio bytes provider for mixing with microphone audio during screen sharing
        call.peerConnectionFactory.setScreenAudioBytesProvider { bytesRequested ->
            call.mediaManager.screenShare.getScreenAudioBytes(bytesRequested)
        }

        // Set up microphone enabled provider to check if microphone should be included in mixing
        call.peerConnectionFactory.setMicrophoneEnabledProvider {
            call.mediaManager.microphone.isEnabled.value
        }
    }

    private var participantsMonitoringJob: Job? = null

    private fun listenToSfuSocket() {
        // cancel any old socket monitoring if needed
        eventJob?.cancel()
        errorJob?.cancel()
        stateJob?.cancel()
        participantsMonitoringJob?.cancel()

        participantsMonitoringJob = coroutineScope.launch {
            call.state.participants.collect {
                subscriber?.setTrackLookupPrefixes(it.associate { it.trackLookupPrefix to it.sessionId })
            }
        }

        // State
        // Start listening to connection state on new SFU connection
        stateJob = coroutineScope.launch {
            sfuConnectionModule.socketConnection.state().collect { sfuSocketState ->
                _sfuSfuSocketState.value = sfuSocketState
                when (sfuSocketState) {
                    is SfuSocketState.Connected ->
                        call.state._connection.value =
                            RealtimeConnection.Connected

                    is SfuSocketState.Connecting ->
                        call.state._connection.value =
                            RealtimeConnection.InProgress

                    else -> {
                        // Ignore it
                    }
                }
            }
        }

        // listen to socket events and errors
        eventJob = coroutineScope.launch {
            sfuConnectionModule.socketConnection.events().collect {
                traceEvent(it)
                clientImpl.fireEvent(it, call.cid)
            }
        }

        // Errors
        errorJob = coroutineScope.launch {
            sfuConnectionModule.socketConnection.errors().collect {
                val reconnectStrategy = it.reconnectStrategy
                logger.d { "[RtcSession#error] reconnectStrategy: $reconnectStrategy" }
                when (reconnectStrategy) {
                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST -> {
                        call.fastReconnect()
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN -> {
                        call.rejoin()
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE -> {
                        call.migrate()
                    }

                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT -> {
                        // We are told to disconnect.
                        sfuConnectionModule.socketConnection.disconnect()
                        logger.d {
                            "[WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT], call_id = ${call.id}"
                        }
                        call.state._connection.value = RealtimeConnection.Disconnected
                    }

                    else -> {
                        // Either null or UNSPECIFIED, do not reconnect
                    }
                }
                logger.e(
                    it.streamError.extractCause()
                        ?: IllegalStateException("Error emitted without a cause on SFU connection."),
                ) { "permanent failure on socket connection" }
            }
        }
    }

    private fun traceEvent(it: SfuDataEvent) = safeCall {
        when (it) {
            is ChangePublishQualityEvent -> {
                sfuTracer.trace(
                    PeerConnectionTraceKey.CHANGE_PUBLISH_QUALITY.value,
                    parser.toJson(it.changePublishQuality),
                )
            }

            is ChangePublishOptionsEvent -> {
                sfuTracer.trace(
                    PeerConnectionTraceKey.CHANGE_PUBLISH_OPTIONS.value,
                    parser.toJson(it.change.publish_options),
                )
            }

            is GoAwayEvent -> {
                sfuTracer.trace(PeerConnectionTraceKey.GO_AWAY.value, parser.toJson(it))
            }

            is CallEndedSfuEvent -> {
                sfuTracer.trace(PeerConnectionTraceKey.CALL_ENDED.value, parser.toJson(it))
            }

            is ErrorEvent -> {
                sfuTracer.trace(PeerConnectionTraceKey.SFU_ERROR.value, parser.toJson(it))
            }

            else -> {
                // Don't trace this event.
            }
        }
    }

    private fun updatePeerState() {
        _peerConnectionStates.value = Pair(
            subscriber?.state?.value,
            publisher?.state?.value,
        )
    }

    /**
     * @param forceRestart - set to true if you want to force restart both ICE connections
     * regardless of their current connection status (even if they are CONNECTED)
     */
    suspend fun reconnect(forceRestart: Boolean) {
        // ice restart
        val subscriberAsync = rtcSessionScope.async {
            subscriber?.let {
                if (!it.isHealthy()) {
                    logger.i { "ice restarting subscriber peer connection" }
                    requestSubscriberIceRestart()
                }
            }
        }

        val publisherAsync = rtcSessionScope.async {
            publisher?.let {
                if (!it.isHealthy() || forceRestart) {
                    logger.i { "ice restarting publisher peer connection (force restart = $forceRestart)" }
                    it.restartIce("it.isHealthy() = ${it.isHealthy()}, forceRestart=$forceRestart")
                }
            }
        }

        awaitAll(subscriberAsync, publisherAsync)
    }

    suspend fun connect(
        reconnectDetails: ReconnectDetails? = null,
        options: List<PublishOption>? = null,
    ) {
        logger.i { "[connect] #sfu; #track; no args" }
        val request = JoinRequest(
            subscriber_sdp = throwawaySubscriberSdpAndOptions(),
            publisher_sdp = throwawayPublisherSdpAndOptions(),
            session_id = sessionId,
            token = sfuToken,
            fast_reconnect = false,
            client_details = clientDetails,
            preferred_publish_options = options ?: emptyList(),
            reconnect_details = reconnectDetails,
            capabilities = call.clientCapabilities.values.toList(),
            source = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
        )
        sfuTracer.trace(
            PeerConnectionTraceKey.JOIN_REQUEST.value,
            safeCallWithDefault(null) {
                request.adapter.toString(request)
            },
        )
        listenToSfuSocket()
        sfuConnectionModule.socketConnection.connect(request)
        sfuConnectionModule.socketConnection.whenConnected {
            sendConnectionTimeStats(reconnectDetails?.strategy)
        }
    }

    private suspend fun sendConnectionTimeStats(reconnectStrategy: WebsocketReconnectStrategy? = null) {
        if (reconnectStrategy == null) {
            sendCallStats(
                report = call.collectStats(),
                connectionTimeSeconds = (System.currentTimeMillis() - call.connectStartTime) / 1000f,
            )
        } else {
            sendCallStats(
                report = call.collectStats(),
                reconnectionTimeSeconds = Pair(
                    (System.currentTimeMillis() - call.reconnectStartTime) / 1000f,
                    reconnectStrategy,
                ),
            )
        }
    }

    private fun setSfuConnectionModule(sfuConnectionModule: SfuConnectionModule) {
        // This is used to switch from a current SFU connection to a new migrated SFU connection
        this@RtcSession.sfuConnectionModule = sfuConnectionModule
        listenToSfuSocket()
    }

    private fun listenToMediaChanges() {
        logger.d { "[trackPublishing] listenToMediaChanges" }
        coroutineScope.launch {
            // update the tracks when the camera or microphone status changes
            call.mediaManager.camera.status.collectLatest {
                val canUserSendVideo = call.state.ownCapabilities.value.contains(
                    OwnCapability.SendVideo,
                )

                if (it == DeviceStatus.Enabled) {
                    val resolution = call.mediaManager.camera.resolution.value
                    if (resolution == null) {
                        logger.d { "Camera resolution is null. This will result in an empty video track." }
                    } else {
                        logger.d { "Camera resolution: $resolution" }
                    }
                    if (canUserSendVideo) {
                        setMuteState(isEnabled = true, TrackType.TRACK_TYPE_VIDEO)
                        val streamId = buildTrackId(TrackType.TRACK_TYPE_VIDEO)

                        val track = publisher?.publishStream(
                            streamId,
                            TrackType.TRACK_TYPE_VIDEO,
                            call.mediaManager.camera.resolution.value,
                        )

                        setLocalTrack(
                            TrackType.TRACK_TYPE_VIDEO,
                            VideoTrack(
                                streamId = streamId,
                                video = track as org.webrtc.VideoTrack,
                            ),
                        )
                    } else {
                        logger.d { "[listenToMediaChanges#enableCamera] No capability to send video." }
                    }
                } else {
                    setMuteState(isEnabled = false, TrackType.TRACK_TYPE_VIDEO)
                    publisher?.unpublishStream(TrackType.TRACK_TYPE_VIDEO)
                }
            }
        }

        coroutineScope.launch {
            call.mediaManager.microphone.status.collectLatest {
                if (it == DeviceStatus.Enabled) {
                    createAndPublishAudioTrack()
                } else {
                    setMuteState(isEnabled = false, TrackType.TRACK_TYPE_AUDIO)
                    publisher?.unpublishStream(TrackType.TRACK_TYPE_AUDIO)
                }
            }
        }

        coroutineScope.launch {
            call.mediaManager.screenShare.status.collectLatest {
                val canUserShareScreen = call.state.ownCapabilities.value.contains(
                    OwnCapability.Screenshare,
                )

                if (it == DeviceStatus.Enabled) {
                    if (canUserShareScreen) {
                        setMuteState(true, TrackType.TRACK_TYPE_SCREEN_SHARE)
                        val streamId = buildTrackId(TrackType.TRACK_TYPE_SCREEN_SHARE)
                        val track = publisher?.publishStream(
                            streamId,
                            TrackType.TRACK_TYPE_SCREEN_SHARE,
                        )

                        setLocalTrack(
                            TrackType.TRACK_TYPE_SCREEN_SHARE,
                            VideoTrack(
                                streamId = streamId,
                                video = track as org.webrtc.VideoTrack,
                            ),
                        )
                    }
                } else {
                    setMuteState(false, TrackType.TRACK_TYPE_SCREEN_SHARE)
                    publisher?.unpublishStream(TrackType.TRACK_TYPE_SCREEN_SHARE)
                }
            }
        }
    }

    private suspend fun connectRtc() {
        logger.d { "[connectRtc] #sfu; #track; no args" }
        val settings = call.state.settings.value

        // update the peer state
        coroutineScope.launch {
            // call update participant subscriptions debounced
            publisher?.let {
                it.state.collect {
                    updatePeerState()
                }
            }
        }
        // step 6 - onNegotiationNeeded will trigger and complete the setup using SetPublisherRequest
        listenToMediaChanges()

        // subscribe to the tracks of other participants
        setVideoSubscriptions(true)
        return
    }

    fun setScreenShareTrack() {
        setLocalTrack(
            TrackType.TRACK_TYPE_SCREEN_SHARE,
            VideoTrack(
                streamId = buildTrackId(TrackType.TRACK_TYPE_SCREEN_SHARE),
                video = call.mediaManager.screenShareTrack,
            ),
        )
    }

    /**
     * Responds to TrackPublishedEvent event
     * @see TrackPublishedEvent
     * @see TrackUnpublishedEvent
     *
     * It gets the participant and updates the tracks
     *
     * Track look is done by sessionId & type
     */
    internal fun updatePublishState(
        userId: String,
        sessionId: String,
        trackType: TrackType,
        videoEnabled: Boolean,
        audioEnabled: Boolean,
        paused: Boolean,
    ) {
        logger.d {
            "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, videoEnabled: $videoEnabled, audioEnabled: $audioEnabled"
        }
        val track = getTrack(sessionId, trackType)
        val participant = call.state.getParticipantBySessionId(sessionId)
        if (participant != null && participant.videoPaused.value != paused) {
            participant._videoPaused.value = paused
        }
        track?.enableVideo(videoEnabled)
        track?.enableAudio(audioEnabled)
    }

    private val atomicCleanup = AtomicUnitCall()

    fun cleanup() = atomicCleanup {
        logger.i { "[cleanup] #sfu; #track; no args" }

        coroutineScope.launch {
            serialProcessor.submit("cleanupSfuConnections") {
                sfuConnectionModule.socketConnection.disconnect()
                sfuConnectionMigrationModule?.socketConnection?.disconnect()
                Unit
            }
        }
        sfuConnectionMigrationModule = null
        subscriber?.clear()

        // cleanup the publisher and subcriber peer connections
        safeCall {
            subscriber?.close()
            publisher?.close(true)
        }

        subscriber = null
        publisher = null

        // cleanup all non-local tracks
        supervisorJob.cancel()

        // Note: Executor cleanup is handled by Call cleanup
    }

    internal val muteState = MutableStateFlow(
        mapOf(
            TrackType.TRACK_TYPE_AUDIO to false,
            TrackType.TRACK_TYPE_VIDEO to false,
            TrackType.TRACK_TYPE_SCREEN_SHARE to false,
        ),
    )

    /**
     * Informs the SFU that you're publishing a given track (publishing vs muted)
     * - when switching SFU we should repeat this info
     * - http calls failing here breaks the call. it should retry as long as the
     * -- error isn't permanent, SFU didn't change, the mute/publish state didn't change
     * -- we cap at 30 retries to prevent endless loops
     */
    private fun setMuteState(isEnabled: Boolean, trackType: TrackType) {
        logger.d { "[setPublishState] #sfu; $trackType isEnabled: $isEnabled" }

        // update the local copy
        val copy = muteState.value.toMutableMap()
        copy[trackType] = isEnabled
        val new = copy.toMap()
        muteState.value = new

        val currentSfu = sfuUrl
        // prevent running multiple of these at the same time
        // if there's already a job active. cancel it
        muteStateSyncJob?.cancel()
        // start a new job
        // this code is a bit more complicated due to the retry behaviour
        muteStateSyncJob = coroutineScope.launch {
            flow {
                val request = UpdateMuteStatesRequest(
                    session_id = sessionId,
                    mute_states = copy.map {
                        TrackMuteState(track_type = it.key, muted = !it.value)
                    },
                )
                val result = updateMuteState(request)
                result.onSuccessSuspend { emit(result.getOrThrow()) }
            }.flowOn(DispatcherProvider.IO).retryWhen { cause, attempt ->
                val sameValue = new == muteState.value
                val sameSfu = currentSfu == sfuUrl
                val isPermanent = isPermanentError(cause)
                val willRetry = !isPermanent && sameValue && sameSfu && attempt < 30
                val delayInMs = if (attempt <= 1) 100L else if (attempt <= 3) 300L else 2500L
                logger.w {
                    "updating mute state failed with error $cause, retry attempt: $attempt. will retry $willRetry in $delayInMs ms"
                }
                delay(delayInMs)
                willRetry
            }.collect()
        }
    }

    private fun isPermanentError(cause: Throwable): Boolean {
        return false
    }

    @VisibleForTesting
    internal fun createSubscriber(): Subscriber {
        logger.i { "[createSubscriber] #sfu; no args" }
        val peerConnection = call.peerConnectionFactory.makeSubscriber(
            coroutineScope = rtcSessionScope,
            configuration = connectionConfiguration,
            sfuClient = sfuConnectionModule.api,
            sessionId = sessionId,
            enableStereo = clientImpl.enableStereoForSubscriber,
            tracer = subscriberTracer,
            rejoin = {
                logger.d { "[createPublisher] rejoin attempt, connection state: ${call.state.connection.value}" }
                if (call.state.connection.value !is RealtimeConnection.Reconnecting) {
                    coroutineScope.launch {
                        serialProcessor.submit("subscriberRejoin") {
                            logger.d {
                                "[createPublisher] rejoin attempt EXECUTE, connection state: ${call.state.connection.value} "
                            } // TODO Rahul, sometimes the code will come here right in the first attempt to call
                            call.rejoin()
                        }
                    }
                }
            },
            fastReconnect = {
                coroutineScope.launch {
                    logger.d { "[createPublisher] Fast reconnect, connection state: ${call.state.connection.value}" }
                    if (call.state.connection.value !is RealtimeConnection.Reconnecting) {
                        logger.d { "[createPublisher] fast reconnect EXECUTE" }
                        call.fastReconnect()
                    }
                }
            },
            onIceCandidateRequest = ::sendIceCandidate,
        )
        return peerConnection
    }

    private suspend fun getDummySdp(dummyPeerConnection: StreamPeerConnection): String {
        val offerResult = dummyPeerConnection.createOffer()
        return if (offerResult !is Success) {
            ""
        } else {
            offerResult.value.description
        }
    }

    private suspend fun throwawayPublisherSdpAndOptions(): String {
        val sdp =
            createDummyPeerConnection(RtpTransceiverDirection.SEND_ONLY)?.let { dummyPublisher ->
                val sdp = getDummySdp(dummyPublisher)
                cleanDummyPeerConnection(dummyPublisher)
                sdp
            } ?: ""
        logger.d { "[throwawayPublisherSdpAndOptions] #sfu; sdp: \n$sdp" }
        return sdp
    }

    private suspend fun throwawaySubscriberSdpAndOptions(): String {
        return createDummyPeerConnection(RtpTransceiverDirection.RECV_ONLY)?.let { dummySubscriber ->
            val sdp = getDummySdp(dummySubscriber)
            cleanDummyPeerConnection(dummySubscriber)
            sdp
        } ?: ""
    }

    private fun createDummyPeerConnection(direction: RtpTransceiverDirection): StreamPeerConnection? {
        if (direction != RtpTransceiverDirection.SEND_ONLY && direction != RtpTransceiverDirection.RECV_ONLY) {
            return null
        }

        val addTempTransceivers = { spc: StreamPeerConnection ->
            val init = spc.buildVideoTransceiverInit(emptyList(), false)
            spc.connection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO)
            spc.connection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)
        }

        return call.peerConnectionFactory.makePeerConnection(
            configuration = PeerConnection.RTCConfiguration(emptyList()),
            type = if (direction == RtpTransceiverDirection.SEND_ONLY) {
                StreamPeerType.PUBLISHER
            } else {
                StreamPeerType.SUBSCRIBER
            },
            mediaConstraints = defaultConstraints,
            debugText = "DummyPeerConnection",
        ).apply {
            addTempTransceivers(this)
        }
    }

    private fun cleanDummyPeerConnection(dummyPeerConnection: StreamPeerConnection?) {
        dummyPeerConnection?.connection?.transceivers?.forEach {
            it.stop()
        }
        dummyPeerConnection?.close()
    }

    @VisibleForTesting
    internal fun createPublisher(publishOptions: List<PublishOption>): Publisher {
        return call.peerConnectionFactory.makePublisher(
            sessionId = sessionId,
            sfuClient = sfuConnectionModule.api,
            mediaManager = call.mediaManager,
            configuration = connectionConfiguration,
            publishOptions = publishOptions,
            coroutineScope = rtcSessionScope,
            mediaConstraints = defaultConstraints,
            onNegotiationNeeded = { _, _ -> },
            tracer = publisherTracer,
            onIceCandidate = ::sendIceCandidate,
            rejoin = {
                logger.d { "[createPublisher] rejoin attempt, connection state: ${call.state.connection.value}" }
                if (call.state.connection.value !is RealtimeConnection.Reconnecting) {
                    coroutineScope.launch {
                        serialProcessor.submit("publisherRejoin") {
                            logger.d {
                                "[createPublisher] rejoin attempt EXECUTE, connection state: ${call.state.connection.value} "
                            } // TODO Rahul, sometimes the code will come here right in the first attempt to call
                            call.rejoin()
                        }
                    }
                }
            },
            fastReconnect = {
                coroutineScope.launch {
                    logger.d { "[createPublisher] Fast reconnect, connection state: ${call.state.connection.value}" }
                    if (call.state.connection.value !is RealtimeConnection.Reconnecting) {
                        coroutineScope.launch {
                            logger.d { "[createPublisher] fast reconnect EXECUTE" }
                            call.fastReconnect()
                        }
                    }
                }
            },
            isHifiAudioEnabled = call.state.settings.value?.audio?.hifiAudioEnabled ?: false,
        )
    }

    private fun buildTrackId(trackTypeVideo: TrackType): String {
        // track prefix is only available after the join response
        val trackType = trackTypeVideo.value
        val trackPrefix = call.state.me.value?.trackLookupPrefix
        val old = "$trackPrefix:$trackType:${(Math.random() * 100).toInt()}"
        return old // UUID.randomUUID().toString()
    }

    /**
     * Tells the SFU which video tracks we want to subscribe to
     * - it sends the resolutions we're displaying the video at so the SFU can decide which track to send
     * - when switching SFU we should repeat this info
     * - http calls failing here breaks the call. (since you won't receive the video)
     * - we should retry continuously until it works and after it continues to fail, raise an error that shuts down the call
     * - we retry when:
     * -- error isn't permanent, SFU didn't change, the mute/publish state didn't change
     * -- we cap at 30 retries to prevent endless loops
     */
    internal fun setVideoSubscriptions(useDefaults: Boolean = false) {
        val participants = call.state.participants.value
        val remoteParticipants = call.state.remoteParticipants.value
        coroutineScope.launch {
            serialProcessor.submit("setVideoSubscriptions") {
                subscriber?.setVideoSubscriptions(
                    trackOverridesHandler,
                    participants,
                    remoteParticipants,
                    useDefaults,
                )
                Unit
            }
        }
        logger.d { "[setVideoSubscriptions] #sfu; #track; useDefaults: $useDefaults" }
    }

    fun handleEvent(event: VideoEvent) {
        logger.i { "[rtc handleEvent] #sfu; event: $event" }
        if (event is SfuDataEvent) {
            coroutineScope.launch {
                serialProcessor.submit("handleSfuDataEvent: ${event.getEventType()}") {
                    if (event is SubscriberOfferEvent) {
                        logger.v { "[onRtcEvent] event: SubscriberOfferEvent" }
                    } else {
                        logger.v { "[onRtcEvent] event: $event" }
                    }

                    when (event) {
                        is JoinCallResponseEvent -> {
                            val participantStates = event.callState.participants.map {
                                call.state.getOrCreateParticipant(it)
                            }
                            call.state.replaceParticipants(participantStates)

                            // Reconcile orphaned tracks for all participants
                            participantStates.forEach { participant ->
                                reconcileOrphanedTracks(participant.sessionId)
                            }

                            sfuConnectionModule.socketConnection.whenConnected {
                                logger.d { "JoinCallResponseEvent sfuConnectionModule.socketConnection.whenConnected" }
                                if (publisher == null) {
                                    publisher = createPublisher(event.publishOptions)
                                }
                                connectRtc()
                                processPendingSubscriberEvents()
                                processPendingPublisherEvents()
                            }
                        }

                        is ChangePublishOptionsEvent -> {
                            logger.v { "[changePublishOptions] ChangePublishOptionsEvent: $event, publisher: $publisher" }
                            publisher?.syncPublishOptions(
                                call.mediaManager.camera.resolution.value,
                                event.change.publish_options,
                            ) ?: let {
                                publisherPendingEvents.add(event)
                            }
                        }

                        is SubscriberOfferEvent -> handleSubscriberOffer(event)
                        // this dynascale event tells the SDK to change the quality of the video it's uploading
                        is ChangePublishQualityEvent -> {
                            event.changePublishQuality.video_senders.forEach {
                                publisher?.changePublishQuality(it)
                            }
                        }

                        is TrackPublishedEvent -> {
                            updatePublishState(
                                userId = event.userId,
                                sessionId = event.sessionId,
                                trackType = event.trackType,
                                videoEnabled = true,
                                audioEnabled = true,
                                paused = false,
                            )
                            // Reconcile orphaned tracks for this participant
                            // The track might have arrived before the participant was created
                            reconcileOrphanedTracks(event.sessionId)
                        }

                        is InboundStateNotificationEvent -> {
                            event.inboundVideoStates.forEach { publishState ->
                                updatePublishState(
                                    userId = publishState.userId,
                                    sessionId = publishState.sessionId,
                                    trackType = publishState.trackType,
                                    videoEnabled = true,
                                    audioEnabled = true,
                                    paused = publishState.paused,
                                )
                            }
                        }

                        is TrackUnpublishedEvent -> {
                            updatePublishState(
                                userId = event.userId,
                                sessionId = event.sessionId,
                                trackType = event.trackType,
                                videoEnabled = false,
                                audioEnabled = false,
                                paused = false,
                            )
                        }

                        is ParticipantJoinedEvent -> {
                            // Reconcile orphaned tracks for the participant that just joined
                            // CallState will create the participant, then we attach any orphaned tracks
                            reconcileOrphanedTracks(event.participant.session_id)
                            // the UI layer will automatically trigger updateParticipantsSubscriptions
                        }

                        is ParticipantLeftEvent -> {
                            subscriber?.participantLeft(event.participant)
                            subscriber?.setVideoSubscriptions(
                                trackOverridesHandler,
                                call.state.participants.value,
                                call.state.remoteParticipants.value,
                            )
                        }

                        is ICETrickleEvent -> {
                            handleIceTrickle(event)
                        }

                        is ICERestartEvent -> {
                            val peerType = event.peerType
                            when (peerType) {
                                PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED -> {
                                    publisher?.restartIce("ICERestartEvent, peerType: PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED") ?: let {
                                        publisherPendingEvents.add(event)
                                    }
                                }

                                PeerType.PEER_TYPE_SUBSCRIBER -> {
                                    requestSubscriberIceRestart()
                                }
                            }
                        }

                        else -> {
                            logger.d { "[onRtcEvent] skipped event: $event" }
                        }
                    }
                    Unit
                }
            }
        }
    }

    private val publisherPendingEventsMutex = Mutex(false)
    private val subscriberPendingEventsMutex = Mutex(false)
    private suspend fun RtcSession.processPendingPublisherEvents() =
        publisherPendingEventsMutex.withLock {
            logger.v {
                "[processPendingPublisherEvents] #sfu; #track; publisherPendingEvents: $publisherPendingEvents"
            }
            for (pendingEvent in publisherPendingEvents) {
                when (pendingEvent) {
                    is ICETrickleEvent -> {
                        handleIceTrickle(pendingEvent)
                    }

                    is ICERestartEvent -> {
                        publisher?.restartIce("ICERestartEvent")
                    }

                    is ChangePublishOptionsEvent -> {
                        publisher?.syncPublishOptions(
                            call.mediaManager.camera.resolution.value,
                            pendingEvent.change.publish_options,
                        )
                    }

                    else -> {
                        logger.w { "Unknown event type: $pendingEvent" }
                    }
                }
            }
            publisherPendingEvents.clear()
        }

    private suspend fun RtcSession.processPendingSubscriberEvents() =
        subscriberPendingEventsMutex.withLock {
            logger.v {
                "[processPendingSubscriberEvents] #sfu; #track; subscriberPendingEvents: $subscriberPendingEvents"
            }
            for (pendingEvent in subscriberPendingEvents) {
                when (pendingEvent) {
                    is ICETrickleEvent -> {
                        handleIceTrickle(pendingEvent)
                    }

                    is ICERestartEvent -> {
                        requestSubscriberIceRestart()
                    }

                    is SubscriberOfferEvent -> {
                        handleSubscriberOffer(pendingEvent)
                    }

                    else -> {
                        logger.w { "Unknown event type: $pendingEvent" }
                    }
                }
            }
            subscriberPendingEvents.clear()
        }

    internal val publisherPendingEvents = Collections.synchronizedList(mutableListOf<VideoEvent>())
    internal val subscriberPendingEvents = Collections.synchronizedList(mutableListOf<VideoEvent>())

    /**
     Section, basic webrtc calls
     */

    /**
     * Whenever onIceCandidateRequest is called we send the ice candidate
     */
    private fun sendIceCandidate(candidate: IceCandidate, peerType: StreamPeerType) {
        coroutineScope.launch {
            serialProcessor.submit("sendIceCandidate") {
                flow {
                    logger.d { "[sendIceCandidate] #sfu; #${peerType.stringify()}; candidate: $candidate" }
                    val iceTrickle = ICETrickle(
                        peer_type = peerType.toPeerType(),
                        ice_candidate = Json.encodeToString(candidate),
                        session_id = sessionId,
                    )
                    logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; iceTrickle: $iceTrickle" }
                    val result = sendIceCandidate(iceTrickle)
                    logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; completed: $result" }
                    emit(result.getOrThrow())
                }.retry(3).catch { logger.w { "sending ice candidate failed" } }.collect()
            }
        }
    }

    @VisibleForTesting
    /**
     * Triggered whenever we receive new ice candidate from the SFU
     */
    suspend fun handleIceTrickle(event: ICETrickleEvent) {
        if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED && publisher == null && sfuConnectionModule.socketConnection.state().value is SfuSocketState.Connected) {
            logger.v {
                "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; publisher is null, adding to pending"
            }
            publisherPendingEvents.add(event)
            return
        }

        if (event.peerType == PeerType.PEER_TYPE_SUBSCRIBER && subscriber == null && sfuConnectionModule.socketConnection.state().value is SfuSocketState.Connected) {
            logger.v {
                "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; subscriber is null, adding to pending"
            }
            subscriberPendingEvents.add(event)
            return
        }
        logger.d {
            "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; candidate: ${event.candidate}"
        }
        val iceCandidate: IceCandidate = Json.decodeFromString(event.candidate)
        val result = if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED) {
            publisher?.handleNewIceCandidate(iceCandidate)
        } else {
            subscriber?.handleNewIceCandidate(iceCandidate)
        }
        logger.v { "[handleTrickle] #sfu; #${event.peerType.stringify()}; result: $result" }
    }

    internal val subscriberSdpAnswer = MutableStateFlow<SessionDescription?>(null)

    @VisibleForTesting
    /**
     * This is called when the SFU sends us an offer
     * - Sets the remote description
     * - Creates an answer
     * - Sets the local description
     * - Sends the answer back to the SFU
     */
    suspend fun handleSubscriberOffer(offerEvent: SubscriberOfferEvent) {
        logger.d { "[handleSubscriberOffer] #sfu; #subscriber; event: $offerEvent" }
        if (subscriber == null) {
            subscriberPendingEvents.add(offerEvent)
            return
        }
        subscriber?.negotiate(offerEvent.sdp)
    }

    internal fun getPublisherTracksForReconnect(): List<TrackInfo> {
        return publisher?.getAnnouncedTracksForReconnect() ?: emptyList()
    }

    /**
     * @return [StateFlow] that holds [RtcStatsReport] that the publisher exposes.
     */
    suspend fun getPublisherStats(): RtcStatsReport? {
        return publisher?.getStats()
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the subscriber exposes.
     */
    suspend fun getSubscriberStats(): RtcStatsReport? {
        return subscriber?.getStats()
    }

    private fun List<Array<Any?>>.toJson(): String {
        val outer = JSONArray()
        for (inner in this) {
            outer.put(JSONArray(inner)) // wraps each Array into its own JSONArray
        }
        return outer.toString() // compact JSON → use .toString(2) for pretty
    }

    internal suspend fun sendCallStats(
        report: CallStatsReport,
        connectionTimeSeconds: Float? = null,
        reconnectionTimeSeconds: Pair<Float, WebsocketReconnectStrategy>? = null,
    ) {
        val result = wrapAPICall {
            val androidThermalState =
                safeCallWithDefault(AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED) {
                    val thermalState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        powerManager?.currentThermalStatus
                    } else {
                        AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED
                    }
                    logger.d { "[sendCallStats] #thermals state: $thermalState" }
                    when (thermalState) {
                        THERMAL_STATUS_NONE -> AndroidThermalState.ANDROID_THERMAL_STATE_NONE
                        THERMAL_STATUS_LIGHT -> AndroidThermalState.ANDROID_THERMAL_STATE_LIGHT
                        THERMAL_STATUS_MODERATE -> AndroidThermalState.ANDROID_THERMAL_STATE_MODERATE
                        THERMAL_STATUS_SEVERE -> AndroidThermalState.ANDROID_THERMAL_STATE_SEVERE
                        THERMAL_STATUS_CRITICAL -> AndroidThermalState.ANDROID_THERMAL_STATE_CRITICAL
                        THERMAL_STATUS_EMERGENCY -> AndroidThermalState.ANDROID_THERMAL_STATE_EMERGENCY
                        THERMAL_STATUS_SHUTDOWN -> AndroidThermalState.ANDROID_THERMAL_STATE_SHUTDOWN
                        else -> AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED
                    }
                }
            val powerSaving = safeCallWithDefault(false) {
                val powerSaveMode = powerManager?.isPowerSaveMode
                logger.d { "[sendCallStats] #powerSaveMode state: $powerSaveMode" }
                powerSaveMode ?: false
            }

            val publisherRtcStats = publisher?.stats()
            val subscriberRtcStats = subscriber?.stats()
            publisherTracer.trace("getstats", publisherRtcStats?.delta)
            subscriberTracer.trace("getstats", subscriberRtcStats?.delta)

            val rtcStats = tracerManager.tracers().flatMap {
                it.take().snapshot.map { it.serialize() }
            }.toMutableList().toJson()

            logger.d { "[sendCallStats] #sfu; #track; rtc_stats: $rtcStats" }

            val sendStatsRequest = SendStatsRequest(
                session_id = sessionId,
                sdk = "stream-android",
                unified_session_id = call.unifiedSessionId,
                sdk_version = BuildConfig.STREAM_VIDEO_VERSION,
                webrtc_version = BuildConfig.STREAM_WEBRTC_VERSION,
                publisher_stats = report.toJson(StreamPeerType.PUBLISHER),
                subscriber_stats = report.toJson(StreamPeerType.SUBSCRIBER),
                rtc_stats = rtcStats,
                encode_stats = publisherRtcStats?.performanceStats ?: emptyList(),
                decode_stats = subscriberRtcStats?.performanceStats ?: emptyList(),
                android = AndroidState(
                    thermal_state = androidThermalState,
                    is_power_saver_mode = powerSaving,
                ),
                telemetry = safeCallWithDefault(null) {
                    if (connectionTimeSeconds != null) {
                        Telemetry(
                            connection_time_seconds = connectionTimeSeconds.toFloat(),
                        )
                    } else if (reconnectionTimeSeconds != null) {
                        Telemetry(
                            reconnection = Reconnection(
                                time_seconds = reconnectionTimeSeconds.first.toFloat(),
                                strategy = reconnectionTimeSeconds.second,
                            ),
                        )
                    } else {
                        null
                    }
                },
            )
            logger.d { "[sendCallStats] #sfu; #track; request: $rtcStats" }
            sfuConnectionModule.api.sendStats(
                sendStatsRequest = sendStatsRequest,
            )
        }

        logger.d {
            "sendStats: " + when (result) {
                is Success -> "Success. Response: ${result.value}. Telemetry: connectionTimeSeconds: $connectionTimeSeconds, reconnectionTimeSeconds: ${reconnectionTimeSeconds?.first}, strategy: ${reconnectionTimeSeconds?.second}"
                is Failure -> "Failure. Reason: ${result.value.message}"
            }
        }
    }

    /***
     * Section, API endpoints
     */

    private suspend fun <T : Any> wrapAPICall(apiCall: suspend () -> T): Result<T> {
        return withContext(coroutineScope.coroutineContext) {
            try {
                val result = apiCall()
                Success(result)
            } catch (e: HttpException) {
                // TODO: understand the error conditions here
                parseError(e)
            } catch (e: RtcException) {
                // TODO: understand the error conditions here
                Failure(
                    io.getstream.result.Error.ThrowableError(
                        e.message ?: "RtcException",
                        e,
                    ),
                )
            } catch (e: IOException) {
                // TODO: understand the error conditions here
                Failure(
                    io.getstream.result.Error.ThrowableError(
                        e.message ?: "IOException",
                        e,
                    ),
                )
            }
        }
    }

    private suspend fun parseError(e: Throwable): Failure {
        return Failure(
            io.getstream.result.Error.ThrowableError(
                "CallClientImpl error needs to be handled",
                e,
            ),
        )
    }

    // send whenever we have a new ice candidate
    private suspend fun sendIceCandidate(request: ICETrickle): Result<ICETrickleResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.api.iceTrickle(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // share what size and which participants we're looking at
    internal suspend fun updateSubscriptions(
        request: UpdateSubscriptionsRequest,
    ): Result<UpdateSubscriptionsResponse> = wrapAPICall {
        logger.v { "[updateSubscriptions] #sfu; #track; request $request" }
        val result = sfuConnectionModule.api.updateSubscriptions(request)
        result.error?.let {
            throw RtcException(error = it, message = it.message)
        }
        result
    }

    // share what size and which participants we're looking at
    suspend fun requestSubscriberIceRestart(): Result<ICERestartResponse> =
        subscriber?.restartIce() ?: Failure(
            io.getstream.result.Error.ThrowableError(
                "Subscriber is null",
                Exception("Subscriber is null"),
            ),
        )

    suspend fun requestPublisherIceRestart(): Result<ICERestartResponse> = wrapAPICall {
        val request = ICERestartRequest(
            session_id = sessionId,
            peer_type = PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED,
        )
        sfuConnectionModule.api.iceRestart(request)
    }

    private suspend fun updateMuteState(request: UpdateMuteStatesRequest): Result<UpdateMuteStatesResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.api.updateMuteStates(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // sets display track visibility
    @Synchronized
    fun updateTrackDimensions(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimensions: VideoDimension = Subscriber.defaultVideoDimension,
        viewportId: String = sessionId,
    ) {
        logger.v {
            "[updateTrackDimensions] #track; #sfu; sessionId: $sessionId, trackType: $trackType, visible: $visible, dimensions: $dimensions"
        }
        subscriber?.setTrackDimension(viewportId, sessionId, trackType, visible, dimensions)
        coroutineScope.launch {
            serialProcessor.submit("updateTrackDimensions") {
                if (sessionId != call.sessionId) {
                    // dimension updated for another participant
                    subscriber?.setVideoSubscriptions(
                        trackOverridesHandler,
                        call.state.participants.value,
                        call.state.remoteParticipants.value,
                    )
                }
                Unit
            }
        }
    }

    private fun listenToSubscriberConnection() {
        subscriberListenJob?.cancel()
        subscriberListenJob = coroutineScope.launch {
            serialProcessor.submit("listenToSubscriberConnection") {
                // call update participant subscriptions debounced
                subscriber?.let {
                    it.state.collect {
                        updatePeerState()
                    }
                }
                Unit
            }
        }
    }

    internal fun currentSfuInfo(): Triple<String, List<TrackSubscriptionDetails>, List<TrackInfo>> {
        val previousSessionId = sessionId
        val currentSubscriptions = subscriber?.subscriptions() ?: emptyList()
        val publisherTracks = getPublisherTracksForReconnect()
        return Triple(previousSessionId, currentSubscriptions, publisherTracks)
    }

    internal suspend fun fastReconnect(reconnectDetails: ReconnectDetails?) {
        // Fast reconnect, send a JOIN request on the same SFU
        // and restart ICE on publisher
        logger.d { "[fastReconnect] Starting fast reconnect." }
        publisherTracer.trace("fastReconnect", reconnectDetails.toString())
        val (previousSessionId, currentSubscriptions, publisherTracks) = currentSfuInfo()
        logger.d { "[fastReconnect] Published tracks: $publisherTracks" }

        val request = JoinRequest(
            subscriber_sdp = throwawaySubscriberSdpAndOptions(),
            publisher_sdp = throwawayPublisherSdpAndOptions(),
            session_id = sessionId,
            token = sfuToken,
            client_details = clientDetails,
            preferred_publish_options = publisher?.currentOptions() ?: emptyList(),
            reconnect_details = reconnectDetails,
            capabilities = call.clientCapabilities.values.toList(),
            source = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
        )
        publisherTracer.trace(PeerConnectionTraceKey.JOIN_REQUEST.value, request.toString())

        logger.d { "Connecting RTC, $request" }
        listenToSfuSocket()
        coroutineScope.launch {
            serialProcessor.submit("fastReconnect") {
                sfuConnectionModule.socketConnection.connect(request)
                sfuConnectionModule.socketConnection.whenConnected {
                    val peerConnectionNotUsable =
                        subscriber?.isFailedOrClosed() == true && publisher?.isFailedOrClosed() == true
                    if (peerConnectionNotUsable) {
                        logger.w { "[fastReconnect] Peer connections are not usable, rejoining." }
                        // We could not reuse the peer connections.
                        call.rejoin()
                    } else {
                        publisher?.restartIce("peerConnection is usable")
                        sendCallStats(
                            report = call.collectStats(),
                            reconnectionTimeSeconds = Pair(
                                (System.currentTimeMillis() - call.reconnectStartTime) / 1000f,
                                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                            ),
                        )
                        setVideoSubscriptions(true)
                    }
                }
            }
        }
    }

    internal fun prepareRejoin() {
        // We are rejoining from the start, we don't want to know.
        stateJob?.cancel()
        eventJob?.cancel()
        errorJob?.cancel()
        runBlocking {
            sfuConnectionModule.socketConnection.disconnect()
        }
        publisher?.close(true)
        subscriber?.close()
    }

    internal fun prepareReconnect() {
        // We are reconnecting from the start, we don't want to know.
        stateJob?.cancel()
        eventJob?.cancel()
        errorJob?.cancel()
    }

    internal fun leaveWithReason(reason: String) {
        val leaveCallRequest = LeaveCallRequest(
            session_id = sessionId,
            reason = reason,
        )
        val request = SfuRequest(leave_call_request = leaveCallRequest)
        coroutineScope.launch {
            serialProcessor.submit("leaveWithReason") {
                sfuConnectionModule.socketConnection.sendEvent(SfuDataRequest(request))
                Unit
            }
        }
    }
}
