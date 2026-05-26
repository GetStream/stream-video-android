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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
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
import io.getstream.video.android.core.call.connection.utils.safeApiCall
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.utils.SessionFatalException
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
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
import io.getstream.video.android.core.events.reporting.ClientEventReporter
import io.getstream.video.android.core.events.reporting.TelemetryModel
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.socket.common.SocketActions
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.core.toJson
import io.getstream.video.android.core.trace.PeerConnectionTraceKey
import io.getstream.video.android.core.trace.TraceSlice
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trace.TracerManager
import io.getstream.video.android.core.trace.serialize
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.SerialProcessor
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.debugOnly
import io.getstream.video.android.core.utils.defaultConstraints
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpTransceiver.RtpTransceiverDirection
import org.webrtc.SessionDescription
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
internal class PeerConnectionNotUsableException :
    Exception("Peer connections are not usable after fast reconnect")

/**
 * Outcome of [RtcSession.connectInternal] — either the SFU socket
 * connected successfully or the attempt failed.
 */
internal sealed class SfuConnectionResult {
    object Connected : SfuConnectionResult()
    data class Failed(val error: Exception) : SfuConnectionResult()
}

/**
 * Outcome of [RtcSession.fastReconnect] — extends [SfuConnectionResult]
 * semantics with a [PeerConnectionStale] case for when the socket
 * connected but peer connections are no longer usable.
 */
internal sealed class FastReconnectResult {
    object Connected : FastReconnectResult()
    object PeerConnectionStale : FastReconnectResult()
    data class Failed(val error: Exception) : FastReconnectResult()
}

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
    internal var sfuName: String,
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
    internal val sfuTracer: Tracer = tracerManager.tracer(
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
            onSfuApiError = { error ->
                if (call.state.connection.value is RealtimeConnection.Disconnected) return@SfuConnectionModule
                val strategy = when (error.code) {
                    // WebSocket signaling channel is broken — try to re-establish it.
                    stream.video.sfu.models.ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST ->
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST
                    // The SFU no longer knows this participant — need a full rejoin.
                    stream.video.sfu.models.ErrorCode.ERROR_CODE_PARTICIPANT_NOT_FOUND,
                    stream.video.sfu.models.ErrorCode.ERROR_CODE_PARTICIPANT_MEDIA_TRANSPORT_FAILURE,
                    stream.video.sfu.models.ErrorCode.ERROR_CODE_PARTICIPANT_RECONNECT_FAILED,
                    stream.video.sfu.models.ErrorCode.ERROR_CODE_CALL_NOT_FOUND,
                    ->
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                    else -> {
                        StreamLog.w("Video:RtcSession") {
                            "[onSfuApiError] Unhandled session-fatal error ${error.code}: ${error.message}"
                        }
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN
                    }
                }
                call.scope.launch {
                    call.reconnect(strategy, "SfuApiError:${error.message}:${error.code}")
                }
            },
            tracer = sfuTracer,
            tokenRepository = TokenRepository(sfuToken),
        )
    },
) {
    private var muteStateSyncJob: Job? = null
    private val oneBasedSessionCounter = sessionCounter + 1

    private var stateJob: Job? = null
    private var eventJob: Job? = null
    internal val socket
        get() = sfuConnectionModule.socketConnection

    private val publisherTracer = tracerManager.tracer("$oneBasedSessionCounter-pub")
    private val subscriberTracer = tracerManager.tracer("$oneBasedSessionCounter-sub")

    private val logger by taggedLogger("Video:RtcSession")
    private val networkStateProvider by lazy {
        clientImpl.coordinatorConnectionModule.networkStateProvider
    }
    private val parser: VideoParser = MoshiVideoParser()

    /**
     * Data class representing a track that arrived before its participant existed.
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
     * ConcurrentHashMap for thread-safe put/remove operations.
     * Compound operations (takeOrphanedTracks) require additional synchronization.
     * Key format: "$sessionId:${trackType.value}"
     */
    private val orphanedTracks = java.util.concurrent.ConcurrentHashMap<String, OrphanedTrack>()

    /**
     * Generates a unique key for the orphaned tracks map.
     */
    private fun orphanedTrackKey(sessionId: String, trackType: TrackType): String =
        "$sessionId:${trackType.value}"

    internal val trackOverridesHandler = TrackOverridesHandler(
        onOverridesUpdate = {
            setVideoSubscriptions()
            call.state._participantVideoEnabledOverrides.value = it.mapValues { it.value.visible }
        },
        logger = logger,
    )

    private fun getTrack(sessionId: String, type: TrackType): MediaTrack? {
        val track = subscriber.value?.getTrack(
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
     * Thread-safe: ConcurrentHashMap.put() is atomic.
     */
    private fun registerOrphanedTrack(sessionId: String, trackType: TrackType, track: MediaTrack) {
        val key = orphanedTrackKey(sessionId, trackType)
        orphanedTracks[key] = OrphanedTrack(sessionId, trackType, track)
        subscriberTracer.trace("orphaned-track", "$sessionId:$trackType")
        logger.i {
            "[registerOrphanedTrack] #orphaned-track; Registered track for sessionId=$sessionId, " +
                "type=$trackType, total orphaned=${orphanedTracks.size}"
        }
    }

    /**
     * Retrieves and removes all orphaned tracks for a specific sessionId.
     * Returns a list of track type to track pairs that can be attached to the participant.
     * Synchronized to prevent concurrent reconciliation of the same tracks.
     */
    private fun takeOrphanedTracks(
        sessionId: String,
    ): List<Pair<TrackType, MediaTrack>> = synchronized(orphanedTracks) {
        val tracks = mutableListOf<Pair<TrackType, MediaTrack>>()

        // Remove all tracks for this sessionId
        val iterator = orphanedTracks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.sessionId == sessionId) {
                tracks.add(entry.value.trackType to entry.value.track)
                iterator.remove()
            }
        }

        if (tracks.isNotEmpty()) {
            logger.i {
                "[takeOrphanedTracks] #orphaned-track; Retrieved ${tracks.size} orphaned tracks " +
                    "for sessionId=$sessionId, remaining orphaned=${orphanedTracks.size}"
            }
        }

        tracks
    }

    /**
     * Reconciles orphaned tracks for a participant by attaching any tracks
     * that arrived before the participant was created.
     */
    private fun reconcileOrphanedTracks(sessionId: String) {
        val tracks = takeOrphanedTracks(sessionId)

        if (tracks.isEmpty()) return

        logger.i {
            "[reconcileOrphanedTracks] #orphaned-track; Reconciling ${tracks.size} orphaned tracks " +
                "for sessionId=$sessionId"
        }

        subscriberTracer.trace("reconcile-tracks", tracks.toString())
        tracks.forEach { (trackType, track) ->
            setTrack(sessionId, trackType, track)
        }
    }

    /**
     * Cleans up a specific orphaned track when the underlying WebRTC track is removed.
     * This handles the case where a WebRTC stream is removed while the track is still orphaned
     * (before participant info arrived). Matches JS SDK's track.addEventListener('ended') cleanup.
     * Thread-safe: ConcurrentHashMap.remove() is atomic.
     *
     * @param sessionId The session ID of the participant.
     * @param trackType The specific track type to cleanup.
     */
    private fun cleanupOrphanedTracksForSessionAndType(sessionId: String, trackType: TrackType) {
        val key = orphanedTrackKey(sessionId, trackType)
        val removed = orphanedTracks.remove(key)
        if (removed != null) {
            logger.i {
                "[cleanupOrphanedTracksForSessionAndType] #orphaned-track; Removed orphaned track " +
                    "for sessionId=$sessionId, trackType=$trackType due to stream removal, " +
                    "remaining orphaned=${orphanedTracks.size}"
            }
        }
    }

    private fun getLocalTrack(type: TrackType): MediaTrack? {
        return getTrack(sessionId, type)
    }

    private fun setLocalTrack(type: TrackType, track: MediaTrack) {
        subscriber.value?.setTrack(sessionId, type, track)
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
        val track = publisher.value?.publishStream(
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

    internal val subscriber: MutableStateFlow<Subscriber?> = MutableStateFlow(null)
    internal val publisher: MutableStateFlow<Publisher?> = MutableStateFlow(null)

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

    private var lifecycleObserver: LifecycleObserver? = null

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

        val sfuConnectionModule: SfuConnectionModule = sfuConnectionModuleProvider.invoke()
        setSfuConnectionModule(sfuConnectionModule)

        subscriber.value = createSubscriber()

        coroutineScope.launch {
            subscriber.value?.streams()?.collect {
                val (sessionId, trackType, track) = it
                logger.d {
                    "[streams] #sfu; #track; sessionId: $sessionId, trackType: $trackType, mediaStream: $track"
                }
                setTrack(sessionId, trackType, track)
            }
        }

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                if (lifecycleObserver == null) {
                    lifecycleObserver = object : DefaultLifecycleObserver {

                        override fun onPause(owner: LifecycleOwner) {
                            super.onPause(owner)
                            sfuTracer.trace("onBackground", null)
                        }

                        override fun onResume(owner: LifecycleOwner) {
                            super.onResume(owner)
                            sfuTracer.trace("onForeground", null)
                        }
                    }
                }
                safeCall {
                    lifecycle.addObserver(lifecycleObserver!!)
                }
            }
        }

        // Listen for removed streams to cleanup orphaned tracks
        coroutineScope.launch {
            subscriber.value?.removedStreams()?.collect { removed ->
                val (sessionId, trackType) = removed
                logger.i {
                    "[removedStreams] #sfu; #track; #orphaned-track; Cleaning up orphaned tracks for " +
                        "sessionId=$sessionId, trackType=$trackType"
                }
                cleanupOrphanedTracksForSessionAndType(sessionId, trackType)
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
    private var iceMonitoringJob: Job? = null

    private fun listenToSfuSocket() {
        // cancel any old socket monitoring if needed
        eventJob?.cancel()
        stateJob?.cancel()
        participantsMonitoringJob?.cancel()

        participantsMonitoringJob = coroutineScope.launch {
            call.state.participants.collect {
                subscriber.value?.setTrackLookupPrefixes(it.associate { it.trackLookupPrefix to it.sessionId })
            }
        }

        /**
         * Monitors [SfuSocketState] transitions and delegates reconnection to
         * [Call.reconnect] — the unified retry loop that handles escalation
         * (FAST → REJOIN, MIGRATE → REJOIN)
         *
         * **Connected** — promotes the call to [RealtimeConnection.Connected]
         * and flushes any pending ICE trickle candidates.
         *
         * **DisconnectedTemporarily** — forwards the SFU-provided strategy
         * straight to [Call.reconnect]. The retry loop inside `reconnect` owns
         * all escalation logic; this layer is intentionally thin.
         *
         * **WebSocketEventLost** — intermediate HealthMonitor state; ignored
         * here because the HealthMonitor will re-attempt the connection.
         */
        stateJob = coroutineScope.launch {
            sfuConnectionModule.socketConnection.state().collect { sfuSocketState ->
                logger.d {
                    "[stateJob] SFU socket: $sfuSocketState | " +
                        "connection: ${call.state.connection.value} ($sfuName)"
                }
                _sfuSfuSocketState.value = sfuSocketState
                when (sfuSocketState) {
                    is SfuSocketState.Connected -> {
                        call.state._connection.value =
                            RealtimeConnection.Connected
                        call.onSfuConnectionEstablished()
                        startIceMonitoring()

                        val pendingTrickleEvents = iceTricklePendingEvents.toList()
                        iceTricklePendingEvents.clear()
                        pendingTrickleEvents.forEach {
                            sendIceCandidate(it.first, it.second)
                        }
                    }

                    is SfuSocketState.Connecting -> {
                        val current = call.state.connection.value
                        if (current !is RealtimeConnection.Reconnecting &&
                            current !is RealtimeConnection.Migrating
                        ) {
                            call.state._connection.value = RealtimeConnection.InProgress
                        }
                    }

                    is SfuSocketState.Disconnected.DisconnectedTemporarily -> {
                        val strategy = sfuSocketState.reconnectStrategy
                        val reason = "SFU:${sfuSocketState.error.message}:$strategy"
                        logger.w { "[stateJob] SFU sent $strategy for $sfuName" }
                        coroutineScope.launch { call.reconnect(strategy, reason) }
                    }

                    is SfuSocketState.Disconnected.WebSocketEventLost -> {
                        logger.w { "[stateJob] HealthMonitor detected event loss for $sfuName — triggering reconnect" }
                        coroutineScope.launch {
                            call.reconnect(
                                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                                "SFU:healthcheck-timeout",
                            )
                        }
                    }

                    else -> { }
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
    }

    private fun startIceMonitoring() {
        if (iceMonitoringJob?.isActive == true) return
        iceMonitoringJob = coroutineScope.launch {
            val badIceStates = setOf(
                PeerConnection.IceConnectionState.DISCONNECTED,
                PeerConnection.IceConnectionState.FAILED,
            )
            val goodIceStates = setOf(
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED,
            )

            fun evaluateIceHealth() {
                val conn = call.state.connection.value
                val pubIce = publisher.value?.iceState?.value
                val subIce = subscriber.value?.iceState?.value

                val pubBad = pubIce != null && pubIce in badIceStates
                val subBad = subIce != null && subIce in badIceStates

                if ((pubBad || subBad) && conn is RealtimeConnection.Connected) {
                    logger.w {
                        "[iceMonitor] ICE degraded (pub=$pubIce, sub=$subIce) — marking Reconnecting"
                    }
                    call.state._connection.value = RealtimeConnection.Reconnecting
                } else if (conn is RealtimeConnection.Reconnecting &&
                    _sfuSfuSocketState.value is SfuSocketState.Connected
                ) {
                    val pubOk = pubIce == null || pubIce in goodIceStates
                    val subOk = subIce == null || subIce in goodIceStates
                    if (pubOk && subOk) {
                        logger.i {
                            "[iceMonitor] ICE recovered (pub=$pubIce, sub=$subIce) — marking Connected"
                        }
                        call.state._connection.value = RealtimeConnection.Connected
                    }
                }
            }

            launch {
                publisher.collect { pub ->
                    pub?.iceState?.collect { evaluateIceHealth() }
                }
            }
            launch {
                subscriber.collect { sub ->
                    sub?.iceState?.collect { evaluateIceHealth() }
                }
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

    /**
     * @param forceRestart - set to true if you want to force restart both ICE connections
     * regardless of their current connection status (even if they are CONNECTED)
     */
    suspend fun reconnect(forceRestart: Boolean) {
        // ice restart
        val subscriberAsync = rtcSessionScope.async {
            subscriber.value?.let {
                if (!it.isHealthy()) {
                    logger.i { "ice restarting subscriber peer connection" }
                    requestSubscriberIceRestart()
                }
            }
        }

        val publisherAsync = rtcSessionScope.async {
            publisher.value?.let {
                if (!it.isHealthy() || forceRestart) {
                    logger.i { "ice restarting publisher peer connection (force restart = $forceRestart)" }
                    it.restartIce("it.isHealthy() = ${it.isHealthy()}, forceRestart=$forceRestart")
                }
            }
        }

        awaitAll(subscriberAsync, publisherAsync)
    }

    /**
     * Public entry point kept for backward compatibility.
     * Delegates to [connectInternal] and throws on failure.
     *
     * Prefer [connectInternal] which returns a typed [SfuConnectionResult]
     * instead of throwing, enabling exhaustive `when` handling.
     */
    @Deprecated(
        message = "Use connectInternal() which returns SfuConnectionResult instead of throwing.",
        replaceWith = ReplaceWith("connectInternal(reconnectDetails, options)"),
    )
    suspend fun connect(
        reconnectDetails: ReconnectDetails? = null,
        options: List<PublishOption>? = null,
    ) {
        when (val result = connectInternal(reconnectDetails, options)) {
            is SfuConnectionResult.Connected -> Unit
            is SfuConnectionResult.Failed -> throw result.error
        }
    }

    /**
     * Connects to the SFU and suspends until the connection is established or
     * fails. Returns a typed [SfuConnectionResult] so callers can react to the
     * outcome without catching exceptions.
     */
    internal suspend fun connectInternal(
        reconnectDetails: ReconnectDetails? = null,
        options: List<PublishOption>? = null,
        telemetryModel: TelemetryModel? = null,
    ): SfuConnectionResult {
        logger.i { "[connectInternal] #sfu; #track; reconnect=${reconnectDetails?.strategy}" }
        val reporter = call.client.state.clientEventReporter
        val telemetryWsEventSessionId = reporter.reportWsJoinInitiated(
            callId = call.id,
            callType = call.type,
            sfuId = sfuName,
            wasPreviouslyConnected = reconnectDetails != null,
        )
        val request = buildJoinRequest(reconnectDetails, options)
        sfuTracer.trace(
            PeerConnectionTraceKey.JOIN_REQUEST.value,
            safeCallWithDefault(null) {
                request.adapter.toString(request)
            },
        )
        listenToSfuSocket()
        sfuConnectionModule.socketConnection.connect(request)
        val terminalState = withTimeoutOrNull(SocketActions.DEFAULT_SOCKET_TIMEOUT) {
            sfuConnectionModule.socketConnection.state().first {
                it is SfuSocketState.Connected || it is SfuSocketState.Disconnected
            }
        }
        return when (terminalState) {
            is SfuSocketState.Connected -> {
                reporter.reportWsJoinCompleted(
                    telemetryWsEventSessionId,
                    success = true,
                    retryCount = 0,
                )

                sendConnectionTimeStats(reconnectDetails?.strategy)
                SfuConnectionResult.Connected
            }
            is SfuSocketState.Disconnected -> {
                val msg = when (terminalState) {
                    is SfuSocketState.Disconnected.DisconnectedTemporarily ->
                        "SFU socket disconnected: ${terminalState.error.message}"
                    is SfuSocketState.Disconnected.DisconnectedPermanently ->
                        "SFU socket permanently disconnected: ${terminalState.error.message}"
                    else -> "SFU socket disconnected"
                }
                logger.w { "[connectInternal] $msg" }
                sfuTracer.trace("connect-failed", msg)
                reporter.reportWsJoinCompleted(
                    telemetryWsEventSessionId,
                    success = false,
                    retryCount = telemetryModel?.retryAttempt ?: 0,
                    failureReason = msg,
                    failureCode = "WS_DISCONNECTED",
                )
                sendCallStats()
                SfuConnectionResult.Failed(Exception(msg))
            }
            else -> {
                sfuTracer.trace("connect-failed", "Connection timed out")
                reporter.reportWsJoinCompleted(
                    telemetryWsEventSessionId,
                    success = false,
                    retryCount = telemetryModel?.retryAttempt ?: 0,
                    failureReason = ClientEventReporter.FailureCodes.SFU_REQUEST_TIMEOUT.message,
                    failureCode = ClientEventReporter.FailureCodes.SFU_REQUEST_TIMEOUT.code,
                )

                sendCallStats()
                SfuConnectionResult.Failed(Exception("SFU connection timed out"))
            }
        }
    }

    private suspend fun buildJoinRequest(
        reconnectDetails: ReconnectDetails?,
        options: List<PublishOption>?,
    ): JoinRequest = JoinRequest(
        subscriber_sdp = throwawaySubscriberSdpAndOptions(),
        publisher_sdp = throwawayPublisherSdpAndOptions(),
        unified_session_id = call.unifiedSessionId,
        session_id = sessionId,
        token = sfuToken,
        fast_reconnect = false,
        client_details = clientDetails,
        preferred_publish_options = options ?: emptyList(),
        reconnect_details = reconnectDetails,
        capabilities = call.clientCapabilities.values.toList(),
        source = ParticipantSource.PARTICIPANT_SOURCE_WEBRTC_UNSPECIFIED,
    )

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

    internal val mediaScope = CoroutineScope(Dispatchers.Default)
    private var cameraStatusJob: Job? = null
    private var microphoneStatusJob: Job? = null
    private var screenShareStatusJob: Job? = null

    private fun listenToMediaChanges() {
        logger.d { "[trackPublishing] listenToMediaChanges" }

        cameraStatusJob?.cancel()
        cameraStatusJob = mediaScope.launch {
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

                        val track = publisher.value?.publishStream(
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
                    publisher.value?.unpublishStream(TrackType.TRACK_TYPE_VIDEO)
                }
            }
        }

        microphoneStatusJob?.cancel()
        microphoneStatusJob = mediaScope.launch {
            call.mediaManager.microphone.status.collectLatest {
                if (it == DeviceStatus.Enabled) {
                    createAndPublishAudioTrack()
                } else {
                    setMuteState(isEnabled = false, TrackType.TRACK_TYPE_AUDIO)
                    publisher.value?.unpublishStream(TrackType.TRACK_TYPE_AUDIO)
                }
            }
        }

        screenShareStatusJob?.cancel()
        screenShareStatusJob = mediaScope.launch {
            call.mediaManager.screenShare.status.collectLatest {
                val canUserShareScreen = call.state.ownCapabilities.value.contains(
                    OwnCapability.Screenshare,
                )

                if (it == DeviceStatus.Enabled) {
                    if (canUserShareScreen) {
                        setMuteState(true, TrackType.TRACK_TYPE_SCREEN_SHARE)
                        val streamId = buildTrackId(TrackType.TRACK_TYPE_SCREEN_SHARE)
                        val track = publisher.value?.publishStream(
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
                    publisher.value?.unpublishStream(TrackType.TRACK_TYPE_SCREEN_SHARE)
                }
            }
        }
    }

    private suspend fun connectRtc() {
        logger.d { "[connectRtc] #sfu; #track; no args" }
        // step 6 - onNegotiationNeeded will trigger and complete the setup using SetPublisherRequest
        publisher.value?.let {
            listenToMediaChanges()
        }
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
        subscriber.value?.clear()

        // cleanup the publisher and subcriber peer connections
        safeCall {
            subscriber.value?.close()
            publisher.value?.close(true)
        }

        subscriber.value = null
        publisher.value = null

        // cleanup orphaned tracks to prevent memory leaks
        // Thread-safe: ConcurrentHashMap.clear() is atomic
        if (orphanedTracks.isNotEmpty()) {
            logger.w {
                "[cleanup] #orphaned-track; Clearing ${orphanedTracks.size} orphaned tracks " +
                    "that were never reconciled"
            }
            orphanedTracks.clear()
        }

        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                if (lifecycleObserver != null) {
                    safeCall {
                        lifecycle.removeObserver(lifecycleObserver!!)
                    }
                    lifecycleObserver = null
                }
            }
        }

        mediaScope.cancel()

        // cleanup all non-local tracks
        supervisorJob.cancel()

        // Note: Executor cleanup is handled by Call cleanup
    }

    private fun hasPublishCapability(): Boolean {
        val capabilities = call.state.ownCapabilities.value
        return capabilities.any {
            it == OwnCapability.SendAudio ||
                it == OwnCapability.SendVideo ||
                it == OwnCapability.Screenshare
        }
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
                val response = updateMuteState(request).getOrThrow()
                response.error?.let {
                    error("Mute state update failed: ${it.message}")
                }
                emit(response)
            }.flowOn(DispatcherProvider.IO).retryWhen { cause, attempt ->
                if (cause is SessionFatalException) return@retryWhen false
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
            sfuConnectionModule = sfuConnectionModule,
            rejoin = {
                // Empty, handled differently
            },
            fastReconnect = {
                // Empty, handled differently
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
                            call.rejoin("Publisher#ERROR_CODE_REQUEST_VALIDATION_FAILED")
                        }
                    }
                }
            },
            fastReconnect = {
                // Empty on purpose
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
                subscriber.value?.setVideoSubscriptions(
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
                                if (publisher.value == null && hasPublishCapability()) {
                                    publisher.value = createPublisher(event.publishOptions)
                                }
                                connectRtc()
                                processPendingSubscriberEvents()
                                publisher.value?.let {
                                    processPendingPublisherEvents()
                                }
                            }
                        }

                        is ChangePublishOptionsEvent -> {
                            logger.v {
                                "[changePublishOptions] ChangePublishOptionsEvent: $event, publisher: ${publisher.value}"
                            }
                            publisher.value?.syncPublishOptions(
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
                                publisher.value?.changePublishQuality(it)
                            }
                        }

                        is TrackPublishedEvent -> {
                            // In large calls, TrackPublishedEvent may include participant info
                            // instead of sending separate ParticipantJoinedEvent
                            if (event.participant != null) {
                                logger.i {
                                    "[TrackPublishedEvent] #orphaned-track; Participant info included, " +
                                        "creating/updating participant for sessionId=${event.sessionId}"
                                }
                                val participantState = call.state.getOrCreateParticipant(
                                    event.participant,
                                )
                                // Reconcile any orphaned tracks that arrived before this event
                                reconcileOrphanedTracks(event.sessionId)
                            }

                            updatePublishState(
                                userId = event.userId,
                                sessionId = event.sessionId,
                                trackType = event.trackType,
                                videoEnabled = true,
                                audioEnabled = true,
                                paused = false,
                            )

                            if (event.trackType == TrackType.TRACK_TYPE_AUDIO) {
                                if (event.sessionId == sessionId) {
                                    val isMicDisabled = !call.mediaManager.microphone.isEnabled.value
                                    if (isMicDisabled) {
                                        setMuteState(isEnabled = false, event.trackType)
                                        publisher.value?.unpublishStream(event.trackType)
                                    }
                                }
                            }

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
                            subscriber.value?.participantLeft(event.participant)
                            subscriber.value?.setVideoSubscriptions(
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
                                    publisher.value?.restartIce("ICERestartEvent, peerType: PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED")
                                        ?: let {
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
                        publisher.value?.restartIce("ICERestartEvent")
                    }

                    is ChangePublishOptionsEvent -> {
                        publisher.value?.syncPublishOptions(
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
    internal val iceTricklePendingEvents: MutableList<Pair<IceCandidate, StreamPeerType>> =
        Collections.synchronizedList(mutableListOf<Pair<IceCandidate, StreamPeerType>>())

    /**
     Section, basic webrtc calls
     */

    /**
     * Whenever onIceCandidateRequest is called we send the ice candidate
     */
    private fun sendIceCandidate(candidate: IceCandidate, peerType: StreamPeerType) {
        if (_sfuSfuSocketState.value is SfuSocketState.Connected) {
            coroutineScope.launch {
                serialProcessor.submit("sendIceCandidate") {
                    logger.d { "[sendIceCandidate] #sfu; #${peerType.stringify()}; candidate: $candidate" }
                    val iceTrickle = ICETrickle(
                        peer_type = peerType.toPeerType(),
                        ice_candidate = Json.encodeToString(candidate),
                        session_id = sessionId,
                    )
                    logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; iceTrickle: $iceTrickle" }
                    val result = sendIceCandidate(iceTrickle)
                    logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; completed: $result" }
                }
            }
        } else {
            iceTricklePendingEvents.add(candidate to peerType)
        }
    }

    @VisibleForTesting
    /**
     * Triggered whenever we receive new ice candidate from the SFU
     */
    suspend fun handleIceTrickle(event: ICETrickleEvent) {
        if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED && publisher.value == null && sfuConnectionModule.socketConnection.state().value is SfuSocketState.Connected) {
            logger.v {
                "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; publisher is null, adding to pending"
            }
            publisherPendingEvents.add(event)
            return
        }

        if (event.peerType == PeerType.PEER_TYPE_SUBSCRIBER && subscriber.value == null && sfuConnectionModule.socketConnection.state().value is SfuSocketState.Connected) {
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
            publisher.value?.handleNewIceCandidate(iceCandidate)
        } else {
            subscriber.value?.handleNewIceCandidate(iceCandidate)
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
        if (subscriber.value == null) {
            subscriberPendingEvents.add(offerEvent)
            return
        }
        subscriber.value?.negotiate(offerEvent.sdp)
    }

    internal fun getPublisherTracksForReconnect(): List<TrackInfo> {
        return publisher.value?.getAnnouncedTracksForReconnect() ?: emptyList()
    }

    /**
     * @return [StateFlow] that holds [RtcStatsReport] that the publisher exposes.
     */
    suspend fun getPublisherStats(): RtcStatsReport? {
        return publisher.value?.getStats()
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the subscriber exposes.
     */
    suspend fun getSubscriberStats(): RtcStatsReport? {
        return subscriber.value?.getStats()
    }

    private fun List<Array<Any?>>.toJson(): String {
        val outer = JSONArray()
        for (inner in this) {
            outer.put(JSONArray(inner)) // wraps each Array into its own JSONArray
        }
        return outer.toString() // compact JSON → use .toString(2) for pretty
    }

    internal suspend fun sendCallStats(
        report: CallStatsReport? = null,
        connectionTimeSeconds: Float? = null,
        reconnectionTimeSeconds: Pair<Float, WebsocketReconnectStrategy>? = null,
    ) {
        val result = safeApiCall {
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

            val publisherRtcStats = publisher.value?.stats()
            val subscriberRtcStats = subscriber.value?.stats()
            publisherTracer.trace("getstats", publisherRtcStats?.delta)
            subscriberTracer.trace("getstats", subscriberRtcStats?.delta)

            val tracerSlices = mutableListOf<TraceSlice>()

            val rtcStats = tracerManager.tracers().flatMap { tracer ->
                val slice = tracer.take()
                tracerSlices.add(slice)
                slice.snapshot.map { it.serialize() }
            }.toMutableList().toJson()

            logger.d { "[sendCallStats] #sfu; #track; rtc_stats: $rtcStats" }

            val sendStatsRequest = SendStatsRequest(
                session_id = sessionId,
                sdk = "stream-android",
                unified_session_id = call.unifiedSessionId,
                sdk_version = BuildConfig.STREAM_VIDEO_VERSION,
                webrtc_version = BuildConfig.STREAM_WEBRTC_VERSION,
                publisher_stats = report?.toJson(StreamPeerType.PUBLISHER) ?: "",
                subscriber_stats = report?.toJson(StreamPeerType.SUBSCRIBER) ?: "",
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
            try {
                sfuConnectionModule.api.sendStats(
                    sendStatsRequest = sendStatsRequest,
                )
            } catch (e: Exception) {
                sfuTracer.trace("send-stats-failed", "${e.message}")
                tracerSlices.forEach { slice -> slice.rollback() }
            }
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

    // send whenever we have a new ice candidate
    private suspend fun sendIceCandidate(request: ICETrickle): Result<ICETrickleResponse> =
        safeApiCall { sfuConnectionModule.api.iceTrickle(request) }

    // share what size and which participants we're looking at
    internal suspend fun updateSubscriptions(
        request: UpdateSubscriptionsRequest,
    ): Result<UpdateSubscriptionsResponse> = safeApiCall {
        logger.v { "[updateSubscriptions] #sfu; #track; request $request" }
        sfuConnectionModule.api.updateSubscriptions(request)
    }

    // share what size and which participants we're looking at
    suspend fun requestSubscriberIceRestart(): Result<ICERestartResponse> =
        subscriber.value?.restartIce() ?: Failure(
            io.getstream.result.Error.ThrowableError(
                "Subscriber is null",
                Exception("Subscriber is null"),
            ),
        )

    suspend fun requestPublisherIceRestart(): Result<ICERestartResponse> = safeApiCall {
        val request = ICERestartRequest(
            session_id = sessionId,
            peer_type = PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED,
        )
        sfuConnectionModule.api.iceRestart(request)
    }

    private suspend fun updateMuteState(request: UpdateMuteStatesRequest): Result<UpdateMuteStatesResponse> =
        safeApiCall { sfuConnectionModule.api.updateMuteStates(request) }

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
        subscriber.value?.setTrackDimension(viewportId, sessionId, trackType, visible, dimensions)
        coroutineScope.launch {
            serialProcessor.submit("updateTrackDimensions") {
                if (sessionId != call.sessionId) {
                    // dimension updated for another participant
                    subscriber.value?.setVideoSubscriptions(
                        trackOverridesHandler,
                        call.state.participants.value,
                        call.state.remoteParticipants.value,
                    )
                }
                Unit
            }
        }
    }

    internal fun currentSfuInfo(): Triple<String, List<TrackSubscriptionDetails>, List<TrackInfo>> {
        val previousSessionId = sessionId
        val currentSubscriptions = subscriber.value?.subscriptions() ?: emptyList()
        val publisherTracks = getPublisherTracksForReconnect()
        return Triple(previousSessionId, currentSubscriptions, publisherTracks)
    }

    internal suspend fun fastReconnect(reconnectDetails: ReconnectDetails?, telemetryModel: TelemetryModel? = null): FastReconnectResult {
        logger.d { "[fastReconnect] Starting fast reconnect." }
        sfuTracer.trace("fastReconnect", reconnectDetails.toString())
        val (_, _, publisherTracks) = currentSfuInfo()
        logger.d { "[fastReconnect] Published tracks: $publisherTracks" }

        val connectResult = connectInternal(
            reconnectDetails,
            publisher.value?.currentOptions(),
            telemetryModel,
        )
        if (connectResult is SfuConnectionResult.Failed) {
            return FastReconnectResult.Failed(connectResult.error)
        }

        val peerConnectionClosed =
            subscriber.value?.isClosed() == true || publisher.value?.isClosed() == true
        if (peerConnectionClosed) {
            logger.w { "[fastReconnect] Peer connection is closed — cannot recover, escalating to rejoin" }
            return FastReconnectResult.PeerConnectionStale
        }

        restartIceAfterFastReconnect()
        setVideoSubscriptions(true)
        return FastReconnectResult.Connected
    }

    /**
     * After a successful fast reconnect the underlying network path may have
     * changed (e.g. WiFi ↔ cellular). Proactively restart ICE on both the
     * publisher and subscriber so fresh candidates are gathered and media
     * can flow over the new path.
     *
     * Subscriber restarts are also issued for completeness — the SFU will
     * typically send a new offer, but an explicit restart guarantees recovery
     * even if that offer is delayed or lost.
     */
    private suspend fun restartIceAfterFastReconnect() {
        publisher.value?.let {
            logger.i { "[fastReconnect] Restarting publisher ICE after fast reconnect" }
            it.restartIce("fastReconnect - network change recovery")
        }
        subscriber.value?.let {
            logger.i { "[fastReconnect] Restarting subscriber ICE after fast reconnect" }
            requestSubscriberIceRestart()
        }
    }

    // Prepares this session for migration to a new SFU without destroying it.
    // Old peer connections stay alive so media keeps flowing during the transition.
    /**
     * Cancels all active background jobs and stops the serial processor so that
     * no stale SFU API calls can trigger unwanted reconnects. Every teardown
     * path (migration, reconnect, rejoin, leave) goes through this first.
     *
     * @param cancelEventJob whether to also cancel [eventJob]. Migration defers
     *   this to [finalizeMigration] because the event loop is still needed while
     *   the new session is being established.
     */
    private fun cancelActiveWork(cancelEventJob: Boolean = true) {
        stateJob?.cancel()
        if (cancelEventJob) eventJob?.cancel()
        iceMonitoringJob?.cancel()
        iceMonitoringJob = null
        muteStateSyncJob?.cancel()
        muteStateSyncJob = null
        participantsMonitoringJob?.cancel()
        participantsMonitoringJob = null
        serialProcessor.stop()
    }

    internal fun enterMigration() {
        cancelActiveWork(cancelEventJob = false)
    }

    // Tears down the old session after migration is confirmed (or timed out).
    // No sendLeaveEvent — matches JS SDK behavior (just close WS, no explicit leave for migration).
    internal fun finalizeMigration() {
        eventJob?.cancel()
        cleanup()
    }

    internal suspend fun prepareRejoin(reason: String) {
        val stats = call.collectStats()
        sendCallStats(stats)

        // Mark disconnected immediately — late ICE candidates will be routed
        // to iceTricklePendingEvents instead of being sent to the now-defunct SFU.
        _sfuSfuSocketState.value = SfuSocketState.Disconnected.Stopped

        // Tell the old SFU we're leaving so it evicts immediately
        // instead of waiting for the reconnect grace period.
        // Must complete before cleanup() cancels the supervisor job.
        sendLeaveEvent(reason)
        cancelActiveWork()
        cleanup()
    }

    /**
     * Simulates an SFU_FULL error for debugging. Injects a network error with code 700
     * and MIGRATE strategy, which triggers immediate migration to a new SFU.
     *
     * Only available in debug builds ([BuildConfig.DEBUG_TOOLS_ENABLED]).
     */
    internal fun simulateSfuFull() = debugOnly {
        logger.w { "[simulateSfuFull] Simulating SFU_FULL for $sfuName" }
        coroutineScope.launch {
            sfuConnectionModule.socketConnection.simulateNetworkError(
                error = io.getstream.result.Error.NetworkError(
                    message = "Simulated SFU_FULL (code 700)",
                    serverErrorCode = 700,
                    statusCode = 700,
                ),
                reconnectStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            )
        }
    }

    internal suspend fun prepareReconnect() {
        cancelActiveWork()
        val currentState = sfuConnectionModule.socketConnection.state().value
        if (currentState is SfuSocketState.Connected || currentState is SfuSocketState.Connecting) {
            sfuConnectionModule.socketConnection.disconnect()
        } else {
            logger.d { "[prepareReconnect] Socket already disconnected ($currentState) — skipping disconnect" }
        }
    }

    /**
     * Sends a leave event to the SFU so it evicts this participant immediately
     * instead of waiting for the reconnect grace period. This must be called
     * **before** [cleanup] to ensure the socket is still open.
     */
    internal suspend fun sendLeaveEvent(reason: String) {
        val leaveCallRequest = LeaveCallRequest(
            session_id = sessionId,
            reason = reason,
        )
        sfuTracer.trace("leave-session", reason)
        val request = SfuRequest(leave_call_request = leaveCallRequest)
        sfuConnectionModule.socketConnection.sendEvent(SfuDataRequest(request))
    }
}
