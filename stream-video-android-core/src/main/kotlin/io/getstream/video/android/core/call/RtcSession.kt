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
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.RtcException
import io.getstream.video.android.core.events.ChangePublishOptionsEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ICERestartEvent
import io.getstream.video.android.core.events.ICETrickleEvent
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
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.core.toJson
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.defaultConstraints
import io.getstream.video.android.core.utils.mapState
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
import kotlinx.coroutines.flow.debounce
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
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpTransceiver
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
import stream.video.sfu.models.Participant
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
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SendStatsRequest
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SetPublisherResponse
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
 */
public class RtcSession internal constructor(
    client: StreamVideo,
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
    private val coroutineScope: CoroutineScope =
        CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob),
    private val sfuConnectionModuleProvider: () -> SfuConnectionModule = {
        SfuConnectionModule(
            context = clientImpl.context,
            apiKey = apiKey,
            apiUrl = sfuUrl,
            wssUrl = sfuWsUrl,
            connectionTimeoutInMs = 2000L,
            userToken = sfuToken,
            lifecycle = lifecycle,
        )
    },
) {

    internal val trackIdToParticipant: MutableStateFlow<Map<String, String>> =
        MutableStateFlow(emptyMap())
    private var syncSubscriberAnswer: Job? = null
    private var syncSubscriberCandidates: Job? = null
    private var syncPublisherJob: Job? = null
    private var subscriptionSyncJob: Job? = null
    private var muteStateSyncJob: Job? = null
    private var subscriberListenJob: Job? = null

    private var videoTransceiverInitialized: Boolean = false
    private var audioTransceiverInitialized: Boolean = false
    private var screenshareTransceiverInitialized: Boolean = false
    private var stateJob: Job? = null
    private var errorJob: Job? = null
    private var eventJob: Job? = null
    internal val socket
        get() = sfuConnectionModule.socketConnection

    private val logger by taggedLogger("Video:RtcSession")
    private val dynascaleLogger by taggedLogger("Video:RtcSession:Dynascale")

    internal val lastVideoStreamAdded = MutableStateFlow<MediaStream?>(null)

    internal val _peerConnectionStates =
        MutableStateFlow<Pair<PeerConnection.PeerConnectionState?, PeerConnection.PeerConnectionState?>?>(
            null,
        )

    // run all calls on a supervisor job so we can easily cancel them

    internal val defaultVideoDimension = VideoDimension(1080, 2340)

    // participants by session id -> participant state
    private val trackPrefixToSessionIdMap =
        call.state.participants.mapState { it.associate { it.trackLookupPrefix to it.sessionId } }

    // We need to update tracks for all participants
    // It's cleaner to store here and have the participant state reference to it
    var tracks: MutableMap<String, MutableMap<TrackType, MediaTrack>> = mutableMapOf()
    val trackDimensions = MutableStateFlow<Map<String, Map<TrackType, TrackDimensions>>>(
        emptyMap(),
    )
    val trackDimensionsDebounced = trackDimensions.debounce(100)
    internal val trackOverridesHandler = TrackOverridesHandler(
        onOverridesUpdate = {
            setVideoSubscriptions()
            call.state._participantVideoEnabledOverrides.value = it.mapValues { it.value.visible }
        },
        logger = logger,
    )

    private fun getTrack(sessionId: String, type: TrackType): MediaTrack? {
        if (!tracks.containsKey(sessionId)) {
            tracks[sessionId] = mutableMapOf()
        }
        return tracks[sessionId]?.get(type)
    }

    private fun setTrack(sessionId: String, type: TrackType, track: MediaTrack) {
        if (!tracks.containsKey(sessionId)) {
            tracks[sessionId] = mutableMapOf()
        }
        tracks[sessionId]?.set(type, track)

        when (type) {
            TrackType.TRACK_TYPE_VIDEO -> {
                call.state.getParticipantBySessionId(sessionId)?.setVideoTrack(track.asVideoTrack())
            }

            TrackType.TRACK_TYPE_AUDIO -> {
                call.state.getParticipantBySessionId(sessionId)?._audioTrack?.value =
                    track.asAudioTrack()
            }

            TrackType.TRACK_TYPE_SCREEN_SHARE, TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO -> {
                call.state.getParticipantBySessionId(sessionId)?._screenSharingTrack?.value =
                    track.asVideoTrack()
            }

            TrackType.TRACK_TYPE_UNSPECIFIED -> {
                logger.w { "Unspecified track type" }
            }
        }
    }

    private fun getLocalTrack(type: TrackType): MediaTrack? {
        return getTrack(sessionId, type)
    }

    private fun setLocalTrack(type: TrackType, track: MediaTrack) {
        return setTrack(sessionId, type, track)
    }

    /**
     * Connection and WebRTC.
     */

    private var iceServers = buildRemoteIceServers(remoteIceServers)

    private val connectionConfiguration: PeerConnection.RTCConfiguration
        get() = buildConnectionConfiguration(iceServers)

    /** subscriber peer connection is used for subs */
    public var subscriber: StreamPeerConnection? = null

    internal var publisher: Publisher? = null

    /** publisher for publishing, using 2 peer connections prevents race conditions in the offer/answer cycle */
    // internal var publisher: StreamPeerConnection? = null

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
        subscriber = createSubscriber()
        // publisher = createPublisher()

        listenToSubscriberConnection()
        val sfuConnectionModule: SfuConnectionModule = sfuConnectionModuleProvider.invoke()
        setSfuConnectionModule(sfuConnectionModule)
        listenToSfuSocket()
        coroutineScope.launch {
            // call update participant subscriptions debounced
            trackDimensionsDebounced.collect {
                logger.v { "<init> #sfu; #track; trackDimensions: $it" }
                setVideoSubscriptions()
            }
        }

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
    }

    private fun listenToSfuSocket() {
        // cancel any old socket monitoring if needed
        eventJob?.cancel()
        errorJob?.cancel()
        stateJob?.cancel()

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
        val subscriberAsync = coroutineScope.async {
            subscriber?.let {
                if (!it.isHealthy()) {
                    logger.i { "ice restarting subscriber peer connection" }
                    requestSubscriberIceRestart()
                }
            }
        }

        val publisherAsync = coroutineScope.async {
            publisher?.let {
                if (!it.isHealthy() || forceRestart) {
                    logger.i { "ice restarting publisher peer connection (force restart = $forceRestart)" }
                    it.restartIce()
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
        )
        logger.d { "Connecting RTC, $request" }
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
            // We don't want to publish video track if there is no camera resolution
            if (call.camera.resolution.value == null) {
                return@launch
            }

            // update the tracks when the camera or microphone status changes
            call.mediaManager.camera.status.collectLatest {
                val canUserSendVideo = call.state.ownCapabilities.value.contains(
                    OwnCapability.SendVideo,
                )

                if (it == DeviceStatus.Enabled) {
                    if (canUserSendVideo) {
                        setMuteState(isEnabled = true, TrackType.TRACK_TYPE_VIDEO)

                        val track = publisher?.publishStream(
                            TrackType.TRACK_TYPE_VIDEO,
                            call.mediaManager.camera.resolution.value,
                        )

                        setLocalTrack(
                            TrackType.TRACK_TYPE_VIDEO,
                            VideoTrack(
                                streamId = buildTrackId(TrackType.TRACK_TYPE_VIDEO),
                                video = track as org.webrtc.VideoTrack,
                            ),
                        )
                    }
                } else {
                    setMuteState(isEnabled = false, TrackType.TRACK_TYPE_VIDEO)
                    publisher?.unpublishStream(TrackType.TRACK_TYPE_VIDEO)
                }
            }
        }

        coroutineScope.launch {
            call.mediaManager.microphone.status.collectLatest {
                val canUserSendAudio = call.state.ownCapabilities.value.contains(
                    OwnCapability.SendAudio,
                )

                if (it == DeviceStatus.Enabled) {
                    if (canUserSendAudio) {
                        setMuteState(isEnabled = true, TrackType.TRACK_TYPE_AUDIO)

                        val track = publisher?.publishStream(
                            TrackType.TRACK_TYPE_AUDIO,
                        )

                        setLocalTrack(
                            TrackType.TRACK_TYPE_AUDIO,
                            AudioTrack(
                                streamId = buildTrackId(TrackType.TRACK_TYPE_AUDIO),
                                audio = track as org.webrtc.AudioTrack,
                            ),
                        )
                    }
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

                        val track = publisher?.publishStream(
                            TrackType.TRACK_TYPE_SCREEN_SHARE,
                        )

                        setLocalTrack(
                            TrackType.TRACK_TYPE_SCREEN_SHARE,
                            VideoTrack(
                                streamId = buildTrackId(TrackType.TRACK_TYPE_SCREEN_SHARE),
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

    /**
     * A single media stream contains multiple tracks. We receive it from the subcriber peer connection
     *
     * Loop over the audio and video tracks
     * Update the local tracks
     *
     * Audio is available from the start.
     * Video only becomes available after we update the subscription
     */
    private fun addStream(mediaStream: MediaStream) {
        val (trackPrefix, trackTypeString) = mediaStream.id.split(':')
        val sessionId = trackPrefixToSessionIdMap.value[trackPrefix]

        if (sessionId == null || trackPrefixToSessionIdMap.value[trackPrefix].isNullOrEmpty()) {
            logger.d { "[addStream] skipping unrecognized trackPrefix $trackPrefix $mediaStream.id" }
            return
        }

        val trackTypeMap = mapOf(
            "TRACK_TYPE_UNSPECIFIED" to TrackType.TRACK_TYPE_UNSPECIFIED,
            "TRACK_TYPE_AUDIO" to TrackType.TRACK_TYPE_AUDIO,
            "TRACK_TYPE_VIDEO" to TrackType.TRACK_TYPE_VIDEO,
            "TRACK_TYPE_SCREEN_SHARE" to TrackType.TRACK_TYPE_SCREEN_SHARE,
            "TRACK_TYPE_SCREEN_SHARE_AUDIO" to TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO,
        )
        val trackType =
            trackTypeMap[trackTypeString] ?: TrackType.fromValue(trackTypeString.toInt())
                ?: throw IllegalStateException("trackType not recognized: $trackTypeString")

        logger.i { "[addStream] #sfu; mediaStream: $mediaStream" }
        mediaStream.audioTracks.forEach { track ->
            logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
            track.setEnabled(true)
            val audioTrack = AudioTrack(
                streamId = mediaStream.id,
                audio = track,
            )
            val current = trackIdToParticipant.value.toMutableMap()
            current[track.id()] = sessionId
            trackIdToParticipant.value = current

            setTrack(sessionId, trackType, audioTrack)
        }

        mediaStream.videoTracks.forEach { track ->
            logger.w { "[addStream] #sfu; #track; videoTrack: ${track.stringify()}" }
            track.setEnabled(true)
            val videoTrack = VideoTrack(
                streamId = mediaStream.id,
                video = track,
            )
            val current = trackIdToParticipant.value.toMutableMap()
            current[track.id()] = sessionId
            trackIdToParticipant.value = current

            setTrack(sessionId, trackType, videoTrack)
        }
        if (mediaStream.videoTracks.isNotEmpty()) {
            lastVideoStreamAdded.value = mediaStream
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
    ) {
        logger.d {
            "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, videoEnabled: $videoEnabled, audioEnabled: $audioEnabled"
        }
        val track = getTrack(sessionId, trackType)
        track?.enableVideo(videoEnabled)
        track?.enableAudio(audioEnabled)
    }

    private val atomicCleanup = AtomicUnitCall()

    fun cleanup() = atomicCleanup {
        logger.i { "[cleanup] #sfu; #track; no args" }

        coroutineScope.launch {
            sfuConnectionModule.socketConnection.disconnect()
            sfuConnectionMigrationModule?.socketConnection?.disconnect()
        }
        sfuConnectionMigrationModule = null

        // cleanup the publisher and subcriber peer connections
        safeCall {
            subscriber?.connection?.close()
            publisher?.close(true)
        }

        subscriber = null
        publisher = null

        // cleanup all non-local tracks
        tracks.filter { it.key != sessionId }.values.map { it.values }.flatten()
            .forEach { wrapper ->
                try {
                    safeCall {
                        wrapper.asAudioTrack()?.audio?.dispose()
                        wrapper.asVideoTrack()?.video?.dispose()
                    }
                } catch (e: Exception) {
                    logger.w { "Error disposing track: ${e.message}" }
                }
            }
        tracks.clear()
        trackDimensions.value = emptyMap()
        supervisorJob.cancel()
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
    public fun createSubscriber(): StreamPeerConnection {
        logger.i { "[createSubscriber] #sfu; no args" }
        val peerConnection = call.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = defaultConstraints,
            onStreamAdded = { addStream(it) }, // addTrack
            onIceCandidateRequest = ::sendIceCandidate,
        )
        peerConnection.connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(
                RtpTransceiver.RtpTransceiverDirection.RECV_ONLY,
            ),
        )

        peerConnection.connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(
                RtpTransceiver.RtpTransceiverDirection.RECV_ONLY,
            ),
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
            spc.connection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)
            spc.connection.addTransceiver(MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO)
        }

        return call.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = PeerConnection.RTCConfiguration(emptyList()),
            type = if (direction == RtpTransceiverDirection.SEND_ONLY) {
                StreamPeerType.PUBLISHER
            } else {
                StreamPeerType.SUBSCRIBER
            },
            mediaConstraints = defaultConstraints,
        ).apply {
            addTempTransceivers(this)
        }
    }

    private fun cleanDummyPeerConnection(dummyPeerConnection: StreamPeerConnection?) {
        dummyPeerConnection?.connection?.transceivers?.forEach {
            it.stop()
        }
        dummyPeerConnection?.connection?.close()
    }

    @VisibleForTesting
    internal fun createPublisher(publishOptions: List<PublishOption>): Publisher {
        return call.peerConnectionFactory.makePublisher(
            sessionId = sessionId,
            me = call.state.me.value!!,
            sfuClient = sfuConnectionModule.api,
            mediaManager = call.mediaManager,
            configuration = connectionConfiguration,
            publishOptions = publishOptions,
            coroutineScope = coroutineScope,
            mediaConstraints = defaultConstraints,
            onNegotiationNeeded = { _, _ -> },
            onIceCandidate = ::sendIceCandidate,
        ) {
            coroutineScope.launch {
                call.rejoin()
            }
        }
    }

    private fun buildTrackId(trackTypeVideo: TrackType): String {
        // track prefix is only available after the join response
        val trackType = trackTypeVideo.value
        val trackPrefix = call.state.me.value?.trackLookupPrefix
        val old = "$trackPrefix:$trackType:${(Math.random() * 100).toInt()}"
        return old // UUID.randomUUID().toString()
    }

    /**
     * This is called when you are look at a different set of participants
     * or at a different size
     *
     * It tells the SFU that we want to receive person a's video at 1080p, and person b at 360p
     *
     * Since the viewmodel knows what's actually displayed
     */
    internal fun defaultTracks(): List<TrackSubscriptionDetails> {
        val sortedParticipants = call.state.participants.value
        val otherParticipants = sortedParticipants.filter { it.sessionId != sessionId }.take(5)
        val tracks = mutableListOf<TrackSubscriptionDetails>()
        otherParticipants.forEach { participant ->
            if (participant.videoEnabled.value) {
                val track = TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = TrackType.TRACK_TYPE_VIDEO,
                    dimension = defaultVideoDimension,
                    session_id = participant.sessionId,
                )
                tracks.add(track)
            }
            if (participant.screenSharingEnabled.value) {
                val track = TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = TrackType.TRACK_TYPE_SCREEN_SHARE,
                    dimension = defaultVideoDimension,
                    session_id = participant.sessionId,
                )
                tracks.add(track)
            }
        }

        return tracks
    }

    internal fun visibleTracks(): List<TrackSubscriptionDetails> {
        val participants = call.state.remoteParticipants.value
        val trackDisplayResolution = trackDimensions.value

        val tracks = participants.map { participant ->
            val trackDisplay = trackDisplayResolution[participant.sessionId] ?: emptyMap()

            trackDisplay.entries.filter { it.value.visible }.map { display ->
                dynascaleLogger.i {
                    "[visibleTracks] $sessionId subscribing ${participant.sessionId} to : ${display.key}"
                }
                TrackSubscriptionDetails(
                    user_id = participant.userId.value,
                    track_type = display.key,
                    dimension = display.value.dimensions,
                    session_id = participant.sessionId,
                )
            }
        }.flatten()
        return tracks
    }

    internal val subscriptions: MutableStateFlow<List<TrackSubscriptionDetails>> = MutableStateFlow(
        emptyList(),
    )

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
        logger.d { "[setVideoSubscriptions] #sfu; #track; useDefaults: $useDefaults" }
        var tracks = if (useDefaults) {
            // default is to subscribe to the top 5 sorted participants
            defaultTracks()
        } else {
            // if we're not using the default, sub to visible tracks
            visibleTracks()
        }.let(trackOverridesHandler::applyOverrides)

        subscriptions.value = tracks
        val currentSfu = sfuUrl

        subscriptionSyncJob?.cancel()
        subscriptionSyncJob = coroutineScope.launch {
            flow {
                val request = UpdateSubscriptionsRequest(
                    session_id = sessionId,
                    tracks = subscriptions.value,
                )
                dynascaleLogger.d {
                    "[setVideoSubscriptions] #sfu; #track; #manual-quality-selection; UpdateSubscriptionsRequest: $request"
                }
                val sessionToDimension = tracks.map { it.session_id to it.dimension }
                dynascaleLogger.v {
                    "[setVideoSubscriptions] #sfu; #track; #manual-quality-selection; Subscribing to: $sessionToDimension"
                }
                val result = updateSubscriptions(request)
                dynascaleLogger.v {
                    "[setVideoSubscriptions] #sfu; #track; #manual-quality-selection; Result: $result"
                }
                emit(result.getOrThrow())
            }.flowOn(DispatcherProvider.IO).retryWhen { cause, attempt ->
                val sameValue = tracks == subscriptions.value
                val sameSfu = currentSfu == sfuUrl
                val isPermanent = isPermanentError(cause)
                val willRetry = !isPermanent && sameValue && sameSfu && attempt < 30
                val delayInMs = if (attempt <= 1) 100L else if (attempt <= 3) 300L else 2500L
                logger.w {
                    "updating subscriptions failed with error $cause, retry attempt: $attempt. will retry $willRetry in $delayInMs ms"
                }
                delay(delayInMs)
                willRetry
            }.collect()
        }
    }

    fun handleEvent(event: VideoEvent) {
        logger.i { "[rtc handleEvent] #sfu; event: $event" }
        if (event is SfuDataEvent) {
            coroutineScope.launch {
                logger.v { "[onRtcEvent] event: $event" }
                when (event) {
                    is JoinCallResponseEvent -> {
                        val participantStates = event.callState.participants.map {
                            call.state.getOrCreateParticipant(it)
                        }
                        call.state.replaceParticipants(participantStates)
                        sfuConnectionModule.socketConnection.whenConnected {
                            publisher = createPublisher(event.publishOptions)
                            processPendingPublisherEvents()
                            connectRtc()
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
                        )
                    }

                    is TrackUnpublishedEvent -> {
                        updatePublishState(
                            userId = event.userId,
                            sessionId = event.sessionId,
                            trackType = event.trackType,
                            videoEnabled = false,
                            audioEnabled = false,
                        )
                    }

                    is ParticipantJoinedEvent -> {
                        // the UI layer will automatically trigger updateParticipantsSubscriptions
                    }

                    is ParticipantLeftEvent -> {
                        removeParticipantTracks(event.participant)
                        removeParticipantTrackDimensions(event.participant)
                    }

                    is ICETrickleEvent -> {
                        handleIceTrickle(event)
                    }

                    is ICERestartEvent -> {
                        val peerType = event.peerType
                        when (peerType) {
                            PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED -> {
                                publisher?.restartIce() ?: let {
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
            }
        }
    }

    private val publisherPendingEventsMutex = Mutex(false)
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
                        publisher?.restartIce()
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

    internal val publisherPendingEvents = Collections.synchronizedList(mutableListOf<VideoEvent>())

    private fun removeParticipantTracks(participant: Participant) {
        tracks.remove(participant.session_id).also {
            if (it == null) {
                logger.e {
                    "[handleEvent] Failed to remove track on ParticipantLeft " + "- track ID: ${participant.session_id}). Tracks: $tracks"
                }
            }
        }
    }

    private fun removeParticipantTrackDimensions(participant: Participant) {
        logger.v { "[removeParticipantTrackDimensions] #sfu; #track; participant: $participant" }
        val newTrackDimensions = trackDimensions.value.toMutableMap()
        newTrackDimensions.remove(participant.session_id).also {
            if (it == null) {
                logger.e {
                    "[handleEvent] Failed to remove track dimension on ParticipantLeft " + "- track ID: ${participant.session_id}). TrackDimensions: $newTrackDimensions"
                }
            }
        }
        trackDimensions.value = newTrackDimensions
    }

    /**
     Section, basic webrtc calls
     */

    /**
     * Whenever onIceCandidateRequest is called we send the ice candidate
     */
    private fun sendIceCandidate(candidate: IceCandidate, peerType: StreamPeerType) {
        coroutineScope.launch {
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

    @VisibleForTesting
    /**
     * Triggered whenever we receive new ice candidate from the SFU
     */
    suspend fun handleIceTrickle(event: ICETrickleEvent) {
        if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED && publisher == null) {
            logger.v {
                "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; publisher is null, adding to pending"
            }
            publisherPendingEvents.add(event)
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
        val subscriber = subscriber ?: return

        syncSubscriberCandidates?.cancel()

        // step 1 - receive the offer and set it to the remote
        val offerDescription = SessionDescription(
            SessionDescription.Type.OFFER,
            offerEvent.sdp,
        )
        subscriber.setRemoteDescription(offerDescription)

        // step 2 - create the answer
        val answerResult = subscriber.createAnswer()
        if (answerResult !is Success) {
            logger.w {
                "[handleSubscriberOffer] #sfu; #subscriber; rejected (createAnswer failed): $answerResult"
            }
            return
        }
        val answerSdp = answerResult.value
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; answerSdp: ${answerSdp.description}" }

        // step 3 - set local description
        val setAnswerResult = subscriber.setLocalDescription(answerSdp)
        if (setAnswerResult !is Success) {
            logger.w {
                "[handleSubscriberOffer] #sfu; #subscriber; rejected (setAnswer failed): $setAnswerResult"
            }
            return
        }
        subscriberSdpAnswer.value = answerSdp
        // TODO: we could handle SFU changes by having a coroutine job per SFU and just cancel it when it switches
        // TODO: retry behaviour could be cleaned up into 3 different extension functions for better readability
        // see: https://www.notion.so/stream-wiki/Video-Development-Guide-fef3ece1c643455889f2c0fdba74a89d
        val currentSfu = sfuUrl

        // prevent running multiple of these at the same time
        // if there's already a job active. cancel it
        syncSubscriberAnswer?.cancel()
        // start a new job
        // this code is a bit more complicated due to the retry behaviour
        syncSubscriberAnswer = coroutineScope.launch {
            flow {
                // step 4 - send the answer
                logger.v { "[handleSubscriberOffer] #sfu; #subscriber; setAnswerResult: $setAnswerResult" }
                val sendAnswerRequest = SendAnswerRequest(
                    PeerType.PEER_TYPE_SUBSCRIBER, answerSdp.description, sessionId,
                )
                val sendAnswerResult = sendAnswer(sendAnswerRequest)
                logger.v { "[handleSubscriberOffer] #sfu; #subscriber; sendAnswerResult: $sendAnswerResult" }
                emit(sendAnswerResult.getOrThrow())

                // setRemoteDescription has been called and everything is ready - we can
                // now start handling the ICE subscriber candidates queue
                syncSubscriberCandidates = coroutineScope.launch {
                    sfuConnectionModule.socketConnection.events().collect { event ->
                        if (event is ICETrickleEvent) {
                            handleIceTrickle(event)
                        }
                    }
                }
            }.flowOn(DispatcherProvider.IO).retryWhen { cause, attempt ->
                val sameValue = answerSdp == subscriberSdpAnswer.value
                val sameSfu = currentSfu == sfuUrl
                val isPermanent = isPermanentError(cause)
                val willRetry = !isPermanent && sameValue && sameSfu && attempt <= 3
                val delayInMs = if (attempt <= 1) 10L else if (attempt <= 2) 30L else 100L
                logger.w {
                    "sendAnswer failed $cause, retry attempt: $attempt. will retry $willRetry in $delayInMs ms"
                }
                delay(delayInMs)
                willRetry
            }.catch {
                logger.e { "setPublisher failed after 3 retries, asking the call monitor to do an ice restart" }
                coroutineScope.launch { call.rejoin() }
            }.collect()
        }
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

    internal suspend fun sendCallStats(
        report: CallStatsReport,
        connectionTimeSeconds: Float? = null,
        reconnectionTimeSeconds: Pair<Float, WebsocketReconnectStrategy>? = null,
    ) {
        val result = wrapAPICall {
            val androidThermalState =
                safeCallWithDefault(AndroidThermalState.ANDROID_THERMAL_STATE_UNSPECIFIED) {
                    val thermalState = powerManager?.currentThermalStatus
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
            sfuConnectionModule.api.sendStats(
                sendStatsRequest = SendStatsRequest(
                    session_id = sessionId,
                    sdk = "stream-android",
                    sdk_version = BuildConfig.STREAM_VIDEO_VERSION,
                    webrtc_version = BuildConfig.STREAM_WEBRTC_VERSION,
                    publisher_stats = report.toJson(StreamPeerType.PUBLISHER),
                    subscriber_stats = report.toJson(StreamPeerType.SUBSCRIBER),
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
                ),
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

    // reply to when we get an offer from the SFU
    internal suspend fun sendAnswer(request: SendAnswerRequest): Result<SendAnswerResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.api.sendAnswer(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
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

    // call after onNegotiation Needed
    private suspend fun setPublisher(request: SetPublisherRequest): Result<SetPublisherResponse> {
        logger.d { "[setPublisher] #sfu; request $request" }
        return wrapAPICall {
            val result = sfuConnectionModule.api.setPublisher(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }
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
    suspend fun requestSubscriberIceRestart(): Result<ICERestartResponse> = wrapAPICall {
        val request = ICERestartRequest(
            session_id = sessionId,
            peer_type = PeerType.PEER_TYPE_SUBSCRIBER,
        )
        sfuConnectionModule.api.iceRestart(request)
    }

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
        dimensions: VideoDimension = defaultVideoDimension,
    ) {
        logger.v {
            "[updateTrackDimensions] #track; #sfu; #manual-quality-selection; sessionId: $sessionId, trackType: $trackType, visible: $visible, dimensions: $dimensions"
        }
        // The map contains all track dimensions for all participants
        dynascaleLogger.d { "updating dimensions $sessionId $visible $dimensions" }

        // first we make a copy of the dimensions
        val trackDimensionsMap = trackDimensions.value.toMutableMap()

        // next we get or create the dimensions for this participants
        val participantTrackDimensions =
            trackDimensionsMap[sessionId]?.toMutableMap() ?: mutableMapOf()

        // last we get the dimensions for this specific track type
        val oldTrack = participantTrackDimensions[trackType] ?: TrackDimensions(
            dimensions = dimensions,
            visible = visible,
        )
        val newTrack = oldTrack.copy(visible = visible, dimensions = dimensions)
        participantTrackDimensions[trackType] = newTrack

        trackDimensionsMap[sessionId] = participantTrackDimensions

        // Updates are debounced
        trackDimensions.value = trackDimensionsMap
    }

    private fun listenToSubscriberConnection() {
        subscriberListenJob?.cancel()
        subscriberListenJob = coroutineScope.launch {
            // call update participant subscriptions debounced
            subscriber?.let {
                it.state.collect {
                    updatePeerState()
                }
            }
        }
    }

    internal fun currentSfuInfo(): Triple<String, List<TrackSubscriptionDetails>, List<TrackInfo>> {
        val previousSessionId = sessionId
        val currentSubscriptions = subscriptions.value
        val publisherTracks = getPublisherTracksForReconnect()
        return Triple(previousSessionId, currentSubscriptions, publisherTracks)
    }

    internal suspend fun fastReconnect(reconnectDetails: ReconnectDetails?) {
        // Fast reconnect, send a JOIN request on the same SFU
        // and restart ICE on publisher
        logger.d { "[fastReconnect] Starting fast reconnect." }
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
        )
        logger.d { "Connecting RTC, $request" }
        listenToSfuSocket()
        coroutineScope.launch {
            sfuConnectionModule.socketConnection.connect(request)
            sfuConnectionModule.socketConnection.whenConnected {
                val peerConnectionNotUsable =
                    subscriber?.isFailedOrClosed() == true && publisher?.isFailedOrClosed() == true
                if (peerConnectionNotUsable) {
                    logger.w { "[fastReconnect] Peer connections are not usable, rejoining." }
                    // We could not reuse the peer connections.
                    call.rejoin()
                } else {
                    publisher?.restartIce()
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

    internal fun prepareRejoin() {
        // We are rejoining from the start, we don't want to know.
        stateJob?.cancel()
        eventJob?.cancel()
        errorJob?.cancel()
        runBlocking {
            sfuConnectionModule.socketConnection.disconnect()
        }
        publisher?.close(true)
        subscriber?.connection?.close()
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
            sfuConnectionModule.socketConnection.sendEvent(SfuDataRequest(request))
        }
    }
}
