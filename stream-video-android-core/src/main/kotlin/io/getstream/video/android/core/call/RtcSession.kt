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
import io.getstream.video.android.core.ScreenShareManager
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.RtcException
import io.getstream.video.android.core.events.CallEndedSfuEvent
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
import io.getstream.video.android.core.utils.SdpSession
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildMediaConstraints
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.mangleSdpUtil
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.core.utils.stringify
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.IOException
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.VideoEvent
import org.webrtc.CameraEnumerationAndroid.CaptureFormat
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters.Encoding
import org.webrtc.RtpTransceiver
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
import stream.video.sfu.models.Sdk
import stream.video.sfu.models.SdkType
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.VideoLayer
import stream.video.sfu.models.VideoQuality
import stream.video.sfu.models.WebsocketReconnectStrategy
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.ICERestartResponse
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SendStatsRequest
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.TrackMuteState
import stream.video.sfu.signal.TrackSubscriptionDetails
import stream.video.sfu.signal.UpdateMuteStatesRequest
import stream.video.sfu.signal.UpdateMuteStatesResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import kotlin.math.absoluteValue
import kotlin.random.Random

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
    private val clientImpl = client as StreamVideoClient

    internal val lastVideoStreamAdded = MutableStateFlow<MediaStream?>(null)

    internal val _peerConnectionStates =
        MutableStateFlow<Pair<PeerConnection.PeerConnectionState?, PeerConnection.PeerConnectionState?>?>(
            null,
        )

    // run all calls on a supervisor job so we can easily cancel them
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

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

    /** publisher for publishing, using 2 peer connections prevents race conditions in the offer/answer cycle */
    internal var publisher: StreamPeerConnection? = null

    private val mediaConstraints: MediaConstraints by lazy {
        buildMediaConstraints()
    }

    private val audioConstraints: MediaConstraints by lazy {
        buildAudioConstraints()
    }

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

    init {
        if (!StreamVideo.isInstalled) {
            throw IllegalArgumentException(
                "SDK hasn't been initialised yet - can't start a RtcSession",
            )
        }
        logger.i { "<init> #sfu; #track; no args" }

        // step 1 setup the peer connections
        subscriber = createSubscriber()
        publisher = createPublisher()

        listenToSubscriberConnection()
        val sfuConnectionModule = SfuConnectionModule(
            context = clientImpl.context,
            apiKey = apiKey,
            apiUrl = sfuUrl,
            wssUrl = sfuWsUrl,
            connectionTimeoutInMs = 2000L,
            userToken = sfuToken,
            lifecycle = lifecycle,
        )
        setSfuConnectionModule(sfuConnectionModule)
        listenToSfuSocket()
        coroutineScope.launch {
            // call update participant subscriptions debounced
            trackDimensionsDebounced.collect {
                logger.v { "<init> #sfu; #track; trackDimensions: $it" }
                setVideoSubscriptions()
            }
        }

        clientImpl.peerConnectionFactory.setAudioSampleCallback { it ->
            call.processAudioSample(it)
        }

        clientImpl.peerConnectionFactory.setAudioRecordDataCallback { audioFormat, channelCount, sampleRate, sampleData ->
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
                if (it is CallEndedSfuEvent) {
                    logger.d { "#rtcsession #events #sfu, event: $it" }
                    call.leave()
                }
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
                        call.rejoin()
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
                    it.connection.restartIce()
                }
            }
        }

        awaitAll(subscriberAsync, publisherAsync)
    }

    suspend fun connect(reconnectDetails: ReconnectDetails? = null) {
        logger.i { "[connect] #sfu; #track; no args" }
        val request = JoinRequest(
            subscriber_sdp = getSubscriberSdp().description,
            session_id = sessionId,
            token = sfuToken,
            fast_reconnect = false,
            client_details = clientDetails,
            reconnect_details = reconnectDetails,
        )
        logger.d { "Connecting RTC, $request" }
        listenToSfuSocket()
        sfuConnectionModule.socketConnection.connect(request)
        sfuConnectionModule.socketConnection.whenConnected {
            connectRtc()
        }
    }

    private fun initializeVideoTransceiver() {
        if (!videoTransceiverInitialized) {
            publisher?.let {
                it.addVideoTransceiver(
                    call.mediaManager.videoTrack,
                    listOf(buildTrackId(TrackType.TRACK_TYPE_VIDEO)),
                    isScreenShare = false,
                )
                videoTransceiverInitialized = true
            }
        }
    }

    private fun setSfuConnectionModule(sfuConnectionModule: SfuConnectionModule) {
        // This is used to switch from a current SFU connection to a new migrated SFU connection
        this@RtcSession.sfuConnectionModule = sfuConnectionModule
        listenToSfuSocket()
    }

    private fun initializeScreenshareTransceiver() {
        if (!screenshareTransceiverInitialized) {
            publisher?.let {
                it.addVideoTransceiver(
                    call.mediaManager.screenShareTrack,
                    listOf(buildTrackId(TrackType.TRACK_TYPE_SCREEN_SHARE)),
                    isScreenShare = true,
                )
                screenshareTransceiverInitialized = true
            }
        }
    }

    private fun initializeAudioTransceiver() {
        if (!audioTransceiverInitialized) {
            publisher?.let {
                it.addAudioTransceiver(
                    call.mediaManager.audioTrack,
                    listOf(buildTrackId(TrackType.TRACK_TYPE_AUDIO)),
                )
                audioTransceiverInitialized = true
            }
        }
    }

    private suspend fun listenToMediaChanges() {
        coroutineScope.launch {
            // update the tracks when the camera or microphone status changes
            call.mediaManager.camera.status.collectLatest {
                // set the mute /unumute status
                setMuteState(isEnabled = it == DeviceStatus.Enabled, TrackType.TRACK_TYPE_VIDEO)

                if (it == DeviceStatus.Enabled) {
                    initializeVideoTransceiver()
                }
            }
        }
        coroutineScope.launch {
            call.mediaManager.microphone.status.collectLatest {
                // set the mute /unumute status
                setMuteState(isEnabled = it == DeviceStatus.Enabled, TrackType.TRACK_TYPE_AUDIO)

                if (it == DeviceStatus.Enabled) {
                    initializeAudioTransceiver()
                }
            }
        }

        coroutineScope.launch {
            call.mediaManager.screenShare.status.collectLatest {
                // set the mute /unumute status
                setMuteState(
                    isEnabled = it == DeviceStatus.Enabled,
                    TrackType.TRACK_TYPE_SCREEN_SHARE,
                )

                if (it == DeviceStatus.Enabled) {
                    initializeScreenshareTransceiver()
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

        // turn of the speaker if needed
        if (settings?.audio?.speakerDefaultOn == false) {
            call.speaker.setVolume(0)
        }

        // if we are allowed to publish, create a peer connection for it
        val canPublish =
            call.state.ownCapabilities.value.any {
                it == OwnCapability.SendAudio || it == OwnCapability.SendVideo
            }

        if (canPublish) {
            publisher = createPublisher()
        } else {
            // enable the publisher if you receive the send audio or send video capability
            coroutineScope.launch {
                call.state.ownCapabilities.collect {
                    if (it.any { it == OwnCapability.SendAudio || it == OwnCapability.SendVideo }) {
                        publisher = createPublisher()
                    }
                }
            }
        }

        // update the peer state
        coroutineScope.launch {
            // call update participant subscriptions debounced
            publisher?.let {
                it.state.collect {
                    updatePeerState()
                }
            }
        }

        if (canPublish) {
            if (publisher == null) {
                throw IllegalStateException(
                    "Cant send audio and video since publisher hasn't been setup to connect",
                )
            } else {
                // step 2 ensure all tracks are setup correctly
                // start capturing the video

                // step 4 add the audio track to the publisher
                setLocalTrack(
                    TrackType.TRACK_TYPE_AUDIO,
                    AudioTrack(
                        streamId = buildTrackId(TrackType.TRACK_TYPE_AUDIO),
                        audio = call.mediaManager.audioTrack,
                    ),
                )

                // step 5 create the video track
                setLocalTrack(
                    TrackType.TRACK_TYPE_VIDEO,
                    VideoTrack(
                        streamId = buildTrackId(TrackType.TRACK_TYPE_VIDEO),
                        video = call.mediaManager.videoTrack,
                    ),
                )

                // render it on the surface. but we need to start this before forwarding it to the publisher
                logger.v { "[createUserTracks] #sfu; videoTrack: ${call.mediaManager.videoTrack.stringify()}" }
                if (call.mediaManager.camera.status.value == DeviceStatus.Enabled) {
                    initializeVideoTransceiver()
                }
                if (call.mediaManager.microphone.status.value == DeviceStatus.Enabled) {
                    initializeAudioTransceiver()
                }
                if (call.mediaManager.screenShare.status.value == DeviceStatus.Enabled) {
                    initializeScreenshareTransceiver()
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

    fun cleanup() {
        logger.i { "[cleanup] #sfu; #track; no args" }

        coroutineScope.launch {
            sfuConnectionModule.socketConnection.disconnect()
            sfuConnectionMigrationModule?.socketConnection?.disconnect()
        }
        sfuConnectionMigrationModule = null

        // cleanup the publisher and subcriber peer connections
        subscriber?.connection?.close()
        publisher?.connection?.close()

        subscriber = null
        publisher = null

        // cleanup all non-local tracks
        tracks.filter { it.key != sessionId }.values.map { it.values }.flatten()
            .forEach { wrapper ->
                try {
                    wrapper.asAudioTrack()?.audio?.dispose()
                    wrapper.asVideoTrack()?.video?.dispose()
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
        val peerConnection = clientImpl.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
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

    private suspend fun getSubscriberSdp(): SessionDescription {
        subscriber?.let { it ->
            val result = it.createOffer()

            return if (result is Success) {
                mangleSdp(result.value)
            } else {
                throw Error("Couldn't create a generic SDP, create offer failed")
            }
        } ?: throw Error("Couldn't create a generic SDP, subscriber isn't setup")
    }

    private fun mangleSdp(sdp: SessionDescription): SessionDescription {
        val settings = call.state.settings.value
        val red = settings?.audio?.redundantCodingEnabled ?: true
        val opus = settings?.audio?.opusDtxEnabled ?: true

        return mangleSdpUtil(sdp, red, opus)
    }

    @VisibleForTesting
    fun createPublisher(): StreamPeerConnection {
        audioTransceiverInitialized = false
        videoTransceiverInitialized = false
        screenshareTransceiverInitialized = false
        logger.i { "[createPublisher] #sfu; no args" }
        val publisher = clientImpl.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::onNegotiationNeeded,
            onIceCandidateRequest = ::sendIceCandidate,
            maxPublishingBitrate = call.state.settings.value?.video?.targetResolution?.bitrate
                ?: 1_200_000,
        )
        logger.i { "[createPublisher] #sfu; publisher: $publisher" }
        return publisher
    }

    private fun buildTrackId(trackTypeVideo: TrackType): String {
        // track prefix is only available after the join response
        val trackType = trackTypeVideo.value
        val trackPrefix = call.state.me.value?.trackLookupPrefix
        val old = "$trackPrefix:$trackType:${(Math.random() * 100).toInt()}"
        return old // UUID.randomUUID().toString()
    }

    /**
     * Change the quality of video we upload when the ChangePublishQualityEvent event is received
     * This is used for dynsacle
     */
    internal fun updatePublishQuality(event: ChangePublishQualityEvent) = synchronized(this) {
        val sender = publisher?.connection?.transceivers?.firstOrNull {
            it.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO
        }?.sender

        if (sender == null) {
            dynascaleLogger.w {
                "Request to change publishing quality not fulfilled due to missing transceivers or sender."
            }
            return@synchronized
        }

        val enabledRids =
            event.changePublishQuality.video_senders.firstOrNull()?.layers?.associate {
                it.name to it.active
            }
        dynascaleLogger.i { "enabled rids: $enabledRids}" }
        val params = sender.parameters
        val updatedEncodings: MutableList<Encoding> = mutableListOf()
        var changed = false
        for (encoding in params.encodings) {
            val shouldEnable = enabledRids?.get(encoding.rid) ?: false
            if (shouldEnable && encoding.active) {
                updatedEncodings.add(encoding)
            } else if (!shouldEnable && !encoding.active) {
                updatedEncodings.add(encoding)
            } else {
                changed = true
                encoding.active = shouldEnable
                updatedEncodings.add(encoding)
            }
        }
        if (changed) {
            dynascaleLogger.i { "Updated publish quality with encodings $updatedEncodings" }
            params.encodings.clear()
            params.encodings.addAll(updatedEncodings)
            sender.parameters = params
        }
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
                    }

                    is SubscriberOfferEvent -> handleSubscriberOffer(event)
                    // this dynascale event tells the SDK to change the quality of the video it's uploading
                    is ChangePublishQualityEvent -> updatePublishQuality(event)

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
                                publisher?.connection?.restartIce()
                            }

                            PeerType.PEER_TYPE_SUBSCRIBER -> {
                                subscriber?.connection?.restartIce()
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

    private fun removeParticipantTracks(participant: Participant) {
        tracks.remove(participant.session_id).also {
            if (it == null) {
                logger.e {
                    "[handleEvent] Failed to remove track on ParticipantLeft " +
                        "- track ID: ${participant.session_id}). Tracks: $tracks"
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
                    "[handleEvent] Failed to remove track dimension on ParticipantLeft " +
                        "- track ID: ${participant.session_id}). TrackDimensions: $newTrackDimensions"
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
        val answerSdp = mangleSdp(answerResult.value)
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

    internal val publisherSdpOffer = MutableStateFlow<SessionDescription?>(null)

    /**
     * https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/negotiationneeded_event
     *
     * Is called whenever a negotiation is needed. Common examples include
     * - Adding a new media stream
     * - Adding an audio Stream
     * - A screenshare track is started
     *
     * Creates a new SDP
     * - And sets it on the localDescription
     * - Enables video simulcast
     * - calls setPublisher
     * - sets setRemoteDescription
     *
     * Retry behaviour is to retry 3 times quickly as long as
     * - the sfu didn't change
     * - the sdp didn't change
     * If that fails ask the call monitor to do an ice restart
     */
    @VisibleForTesting
    fun onNegotiationNeeded(
        peerConnection: StreamPeerConnection,
        peerType: StreamPeerType,
    ) {
        val id = Random.nextInt().absoluteValue
        logger.d { "[negotiate] #$id; #sfu; #${peerType.stringify()}; peerConnection: $peerConnection" }
        coroutineScope.launch {
            // step 1 - create a local offer
            val offerResult = peerConnection.createOffer()
            if (offerResult !is Success) {
                logger.w {
                    "[negotiate] #$id; #sfu; #${peerType.stringify()}; rejected (createOffer failed): $offerResult"
                }
                return@launch
            }
            val mangledSdp = mangleSdp(offerResult.value)

            // step 2 -  set the local description
            val result = peerConnection.setLocalDescription(mangledSdp)
            if (result.isFailure) {
                // the call health monitor will end up restarting the peer connection and recover from this
                logger.w { "[negotiate] #$id; #sfu; #${peerType.stringify()}; setLocalDescription failed: $result" }
                return@launch
            }

            // step 3 - create the list of tracks
            val tracks = getPublisherTracks(mangledSdp.description)
            val currentSfu = sfuUrl

            publisherSdpOffer.value = mangledSdp
            synchronized(this) {
                syncPublisherJob?.cancel()
                syncPublisherJob = coroutineScope.launch {
                    var attempt = 0
                    val maxAttempts = 3
                    val delayInMs = listOf(10L, 30L, 100L)

                    while (attempt <= maxAttempts - 1) {
                        try {
                            // step 4 - send the tracks and SDP
                            val request = SetPublisherRequest(
                                sdp = mangledSdp.description,
                                session_id = sessionId,
                                tracks = tracks,
                            )

                            val result = setPublisher(request)
                            // step 5 - set the remote description

                            peerConnection.setRemoteDescription(
                                SessionDescription(
                                    SessionDescription.Type.ANSWER, result.getOrThrow().sdp,
                                ),
                            )

                            // If successful, emit the result and break the loop
                            result.getOrThrow()
                            break
                        } catch (cause: Throwable) {
                            val sameValue = mangledSdp == publisherSdpOffer.value
                            val sameSfu = currentSfu == sfuUrl
                            val isPermanent = isPermanentError(cause)
                            val willRetry =
                                !isPermanent && sameValue && sameSfu && attempt < maxAttempts

                            logger.w {
                                "onNegotationNeeded setPublisher failed $cause, retry attempt: $attempt. will retry $willRetry in ${delayInMs[attempt]} ms"
                            }

                            if (willRetry) {
                                delay(delayInMs[attempt])
                                attempt++
                            } else {
                                logger.e {
                                    "setPublisher failed after $attempt retries, asking the call monitor to do an ice restart"
                                }
                                coroutineScope.launch { call.rejoin() }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun getPublisherTracks(sdp: String): List<TrackInfo> {
        val captureResolution = call.camera.resolution.value
        val screenShareTrack = getLocalTrack(TrackType.TRACK_TYPE_SCREEN_SHARE)

        val transceivers = publisher?.connection?.transceivers?.toList() ?: emptyList()
        val tracks = transceivers.filter {
            it.direction == RtpTransceiver.RtpTransceiverDirection.SEND_ONLY && it.sender?.track() != null
        }.map { transceiver ->
            val track = transceiver.sender.track()!!

            val trackType = convertKindToTrackType(track, screenShareTrack)

            val layers: List<VideoLayer> = if (trackType == TrackType.TRACK_TYPE_VIDEO) {
                checkNotNull(captureResolution) {
                    throw IllegalStateException(
                        "video capture needs to be enabled before adding the local track",
                    )
                }
                createVideoLayers(transceiver, captureResolution)
            } else if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
                createScreenShareLayers(transceiver)
            } else {
                emptyList()
            }

            TrackInfo(
                track_id = track.id(),
                track_type = trackType,
                layers = layers,
                mid = transceiver.mid ?: extractMid(
                    sdp,
                    track,
                    screenShareTrack,
                    trackType,
                    transceivers,
                ),
            )
        }
        return tracks
    }

    private fun convertKindToTrackType(track: MediaStreamTrack, screenShareTrack: MediaTrack?) =
        when (track.kind()) {
            "audio" -> TrackType.TRACK_TYPE_AUDIO
            "screen" -> TrackType.TRACK_TYPE_SCREEN_SHARE
            "video" -> {
                // video tracks and screenshare tracks in webrtc are both video
                // (the "screen" track type doesn't seem to be used).
                if (screenShareTrack?.asVideoTrack()?.video?.id() == track.id()) {
                    TrackType.TRACK_TYPE_SCREEN_SHARE
                } else {
                    TrackType.TRACK_TYPE_VIDEO
                }
            }

            else -> TrackType.TRACK_TYPE_UNSPECIFIED
        }

    private fun extractMid(
        sdp: String?,
        track: MediaStreamTrack,
        screenShareTrack: MediaTrack?,
        trackType: TrackType,
        transceivers: List<RtpTransceiver>,
    ): String {
        if (sdp.isNullOrBlank()) {
            logger.w { "[extractMid] No SDP found. Returning empty mid" }
            return ""
        }

        logger.d {
            "[extractMid] No 'mid' found for track. Trying to find it from the Offer SDP"
        }

        val sdpSession = SdpSession()
        sdpSession.parse(sdp)
        val media = sdpSession.media.find { m ->
            m.mline?.type == track.kind() &&
                // if `msid` is not present, we assume that the track is the first one
                (m.msid?.equals(track.id()) ?: true)
        }

        if (media?.mid == null) {
            logger.d {
                "[extractMid] No mid found in SDP for track type ${track.kind()} and id ${track.id()}. Attempting to find a heuristic mid"
            }

            val heuristicMid = transceivers.firstOrNull {
                convertKindToTrackType(track, screenShareTrack) == trackType
            }
            if (heuristicMid != null) {
                return heuristicMid.mid
            }

            logger.d { "[extractMid] No heuristic mid found. Returning empty mid" }
            return ""
        }

        return media.mid.toString()
    }

    private fun createVideoLayers(
        transceiver: RtpTransceiver,
        captureResolution: CaptureFormat,
    ): List<VideoLayer> {
        // we tell the Sfu which resolutions we're sending
        return transceiver.sender.parameters.encodings.map {
            val scaleBy = it.scaleResolutionDownBy ?: 1.0
            val width = captureResolution.width.div(scaleBy) ?: 0
            val height = captureResolution.height.div(scaleBy) ?: 0
            val quality = ridToVideoQuality(it.rid)

            // We need to divide by 1000 because the the FramerateRange is multiplied
            // by 1000 (see javadoc).
            val fps = (captureResolution.framerate?.max ?: 0).div(1000)

            VideoLayer(
                rid = it.rid ?: "",
                video_dimension = VideoDimension(
                    width = width.toInt(),
                    height = height.toInt(),
                ),
                bitrate = it.maxBitrateBps ?: 0,
                fps = fps,
                quality = quality,
            )
        }
    }

    private fun createScreenShareLayers(transceiver: RtpTransceiver): List<VideoLayer> {
        return transceiver.sender.parameters.encodings.map {
            // So far we use hardcoded parameters for screen-sharing. This is aligned
            // with iOS.

            VideoLayer(
                rid = "q",
                video_dimension = VideoDimension(
                    width = ScreenShareManager.screenShareResolution.width,
                    height = ScreenShareManager.screenShareResolution.height,
                ),
                bitrate = ScreenShareManager.screenShareBitrate,
                fps = ScreenShareManager.screenShareFps,
                quality = VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED,
            )
        }
    }

    private fun ridToVideoQuality(rid: String?) =
        when (rid) {
            "f" -> {
                VideoQuality.VIDEO_QUALITY_HIGH
            }

            "h" -> {
                VideoQuality.VIDEO_QUALITY_MID
            }

            else -> {
                VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED
            }
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

    internal suspend fun sendCallStats(report: CallStatsReport) {
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
                ),
            )
        }

        logger.d {
            "sendStats: " + when (result) {
                is Success -> "Success"
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
    private suspend fun sendAnswer(request: SendAnswerRequest): Result<SendAnswerResponse> =
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
    private suspend fun updateSubscriptions(
        request: UpdateSubscriptionsRequest,
    ): Result<UpdateSubscriptionsResponse> =
        wrapAPICall {
            logger.v { "[updateSubscriptions] #sfu; #track; request $request" }
            val result = sfuConnectionModule.api.updateSubscriptions(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // share what size and which participants we're looking at
    suspend fun requestSubscriberIceRestart(): Result<ICERestartResponse> =
        wrapAPICall {
            val request = ICERestartRequest(
                session_id = sessionId,
                peer_type = PeerType.PEER_TYPE_SUBSCRIBER,
            )
            sfuConnectionModule.api.iceRestart(request)
        }

    suspend fun requestPublisherIceRestart(): Result<ICERestartResponse> =
        wrapAPICall {
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
        val publisherTracks = publisher?.let { pub ->
            if (pub.connection.remoteDescription != null) {
                getPublisherTracks(pub.connection.localDescription.description)
            } else {
                null
            }
        } ?: emptyList()
        return Triple(previousSessionId, currentSubscriptions, publisherTracks)
    }

    internal suspend fun fastReconnect(reconnectDetails: ReconnectDetails?) {
        // Fast reconnect, send a JOIN request on the same SFU
        // and restart ICE on publisher
        logger.d { "[fastReconnect] Starting fast reconnect." }
        val (previousSessionId, currentSubscriptions, publisherTracks) = currentSfuInfo()
        logger.d { "[fastReconnect] Published tracks: $publisherTracks" }
        val request = JoinRequest(
            subscriber_sdp = getSubscriberSdp().description,
            session_id = sessionId,
            token = sfuToken,
            client_details = clientDetails,
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
                    subscriber?.connection?.restartIce()
                    publisher?.connection?.restartIce()
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
        publisher?.connection?.close()
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
