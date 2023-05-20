/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

import androidx.annotation.VisibleForTesting
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CameraDirection
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.errors.RtcException
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildMediaConstraints
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.mangleSdpUtil
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.stringify
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openapitools.client.models.OwnCapability
import org.openapitools.client.models.VideoEvent
import org.openapitools.client.models.VideoSettings
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import retrofit2.HttpException
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.VideoLayer
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
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
    var visible: Boolean = false
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
    private val client: StreamVideo,
    private val connectionModule: ConnectionModule,
    private val call: Call,
    internal var sfuUrl: String,
    internal var sfuToken: String,
    internal var remoteIceServers: List<IceServer>,
) {

    private var errorJob: Job? = null
    private var eventJob: Job? = null
    internal val socket by lazy { sfuConnectionModule.sfuSocket }

    private var lastTracks: List<TrackSubscriptionDetails>? = null
    private val context = client.context
    private val logger by taggedLogger("Call:RtcSession")
    private val dynascaleLogger by taggedLogger("Call:RtcSession:Dynascale")
    private val clientImpl = client as StreamVideoImpl

    internal val lastVideoStreamAdded = MutableStateFlow<MediaStream?>(null)

    internal val _peerConnectionStates = MutableStateFlow<Pair<PeerConnection.PeerConnectionState?, PeerConnection.PeerConnectionState?>?>(null)

    internal val sessionId = clientImpl.sessionId

    val trackDimensions =
        MutableStateFlow<Map<String, Map<TrackType, TrackDimensions>>>(
            emptyMap()
        )
    val trackDimensionsDebounced = trackDimensions.debounce(100)

    // run all calls on a supervisor job so we can easily cancel them
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    /**
     * We can't publish tracks till we've received the join event response
     */
    private val joinEventResponse: MutableStateFlow<JoinCallResponseEvent?> = MutableStateFlow(null)

    // participants by session id -> participant state
    private val trackPrefixToSessionIdMap =
        call.state.participants.mapState { it.associate { it.trackLookupPrefix to it.sessionId } }

    // We need to update tracks for all participants
    // It's cleaner to store here and have the participant state reference to it
    var tracks: MutableMap<String, MutableMap<TrackType, MediaTrack>> = mutableMapOf()

    fun getTrack(sessionId: String, type: TrackType): MediaTrack? {
        if (!tracks.containsKey(sessionId)) {
            tracks[sessionId] = mutableMapOf()
        }
        return tracks[sessionId]?.get(type)
    }

    fun setTrack(sessionId: String, type: TrackType, track: MediaTrack) {
        if (!tracks.containsKey(sessionId)) {
            tracks[sessionId] = mutableMapOf()
        }
        tracks[sessionId]?.set(type, track)

        when (type) {
            TrackType.TRACK_TYPE_VIDEO -> {
                call.state.getParticipantBySessionId(sessionId)?._videoTrack?.value =
                    track.asVideoTrack()
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

    fun getLocalTrack(type: TrackType): MediaTrack? {
        return getTrack(sessionId, type)
    }

    fun setLocalTrack(type: TrackType, track: MediaTrack) {
        return setTrack(sessionId, type, track)
    }

    /**
     * Connection and WebRTC.
     */

    private var iceServers = buildRemoteIceServers(remoteIceServers)

    private val connectionConfiguration: PeerConnection.RTCConfiguration by lazy {
        buildConnectionConfiguration(iceServers)
    }

    /** subscriber peer connection is used for subs */
    public var subscriber: StreamPeerConnection? = null

    /** publisher for publishing, using 2 peer connections prevents race conditions in the offer/answer cycle */
    @VisibleForTesting
    var publisher: StreamPeerConnection? = null

    private val mediaConstraints: MediaConstraints by lazy {
        buildMediaConstraints()
    }

    private val audioConstraints: MediaConstraints by lazy {
        buildAudioConstraints()
    }

    internal var sfuConnectionModule: SfuConnectionModule

    init {
        val dataStore = StreamUserDataStore.instance()
        val user = dataStore.user.value
        val apiKey = dataStore.apiKey.value
        if (apiKey.isBlank() || user?.id.isNullOrBlank()) {
            throw IllegalArgumentException("The API key, user ID and token cannot be empty!")
        }

        // step 1 setup the peer connections
        subscriber = createSubscriber()

        coroutineScope.launch {
            // call update participant subscriptions debounced
            subscriber?.let {
                it.state.collect {
                    updatePeerState()
                }
            }
        }

        val session = this
        val getSdp = suspend {
            session.getSubscriberSdp().description
        }
        sfuConnectionModule =
            connectionModule.createSFUConnectionModule(sfuUrl, sessionId, sfuToken, getSdp)
        listenToSocket()

        coroutineScope.launch {
            // call update participant subscriptions debounced
            trackDimensionsDebounced.collect {
                updateParticipantSubscriptions()
            }
        }
    }

    private fun listenToSocket() {
        // cancel any old socket monitoring if needed
        eventJob?.cancel()
        errorJob?.cancel()

        // listen to socket events and errors
        eventJob = coroutineScope.launch {
            sfuConnectionModule.sfuSocket.events.collect() {
                clientImpl.fireEvent(it, call.cid)
            }
        }
        errorJob = coroutineScope.launch {
            sfuConnectionModule.sfuSocket.errors.collect() {
                if (clientImpl.developmentMode) {
                    throw it
                } else {
                    logger.e(it) { "permanent failure on socket connection" }
                }
            }
        }
    }

    private fun updatePeerState() {
        _peerConnectionStates.value = Pair(
            subscriber?.state?.value,
            publisher?.state?.value
        )
    }

    suspend fun reconnect() {
        // ice restart
        subscriber?.connection?.restartIce()
        publisher?.connection?.restartIce()
    }

    suspend fun connect() {
        val timer = clientImpl.debugInfo.trackTime("sfu ws")
        sfuConnectionModule.sfuSocket.connect()
        timer.finish()

        // ensure that the join event has been handled before starting RTC
        joinEventResponse.first { it != null }
        connectRtc()
    }

    suspend fun listenToMediaChanges() {
        coroutineScope.launch {
            // update the tracks when the camera or microphone status changes
            call.mediaManager.camera.status.collectLatest {
                // set the mute /unumute status
                setMuteState(isEnabled = it == DeviceStatus.Enabled, TrackType.TRACK_TYPE_VIDEO)
            }
        }
        coroutineScope.launch {
            call.mediaManager.microphone.status.collectLatest {
                // set the mute /unumute status
                setMuteState(isEnabled = it == DeviceStatus.Enabled, TrackType.TRACK_TYPE_AUDIO)
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
    internal fun addStream(mediaStream: MediaStream) {

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

        logger.i { "[] #sfu; mediaStream: $mediaStream" }
        mediaStream.audioTracks.forEach { track ->
            logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
            track.setEnabled(true)
            val audioTrack = AudioTrack(
                streamId = mediaStream.id,
                audio = track
            )
            setTrack(sessionId, trackType, audioTrack)
        }

        mediaStream.videoTracks.forEach { track ->
            track.setEnabled(true)
            val videoTrack = VideoTrack(
                streamId = mediaStream.id,
                video = track
            )
            setTrack(sessionId, trackType, videoTrack)
        }
        if (sessionId != this.sessionId && mediaStream.videoTracks.isNotEmpty()) {
            lastVideoStreamAdded.value = mediaStream
        }
    }

    suspend fun connectRtc() {
        val settings = call.state.settings.value
        val timer = clientImpl.debugInfo.trackTime("connectRtc")

        // turn of the speaker if needed
        if (settings?.audio?.speakerDefaultOn == false) {
            call.mediaManager.speaker.setVolume(0)
        }

        // if we are allowed to publish, create a peer connection for it
        val canPublish =
            call.state.ownCapabilities.value.any { it == OwnCapability.sendAudio || it == OwnCapability.sendVideo }

        if (canPublish) {
            publisher = createPublisher()
            timer.split("createPublisher")
        } else {
            // enable the publisher if you receive the send audio or send video capability
            coroutineScope.launch {
                call.state.ownCapabilities.collect {
                    if (it.any { it == OwnCapability.sendAudio || it == OwnCapability.sendVideo }) {
                        publisher = createPublisher()
                        timer.split("createPublisher")
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
                throw IllegalStateException("Cant send audio and video since publisher hasn't been setup to connect")
            }
            publisher?.let { publisher ->
                // step 2 ensure all tracks are setup correctly
                // start capturing the video

                // if there is no preview and the camera hasn't been selected by the user fallback to settings
                if (call.mediaManager.camera.status.value == DeviceStatus.NotSelected) {
                    val enabled = settings?.video?.cameraDefaultOn == true
                    call.mediaManager.camera.setEnabled(enabled)
                    // check the settings if we should default to front or back facing camera
                    val defaultDirection = if (settings?.video?.cameraFacing == VideoSettings.CameraFacing.front) {
                        CameraDirection.Front
                    } else {
                        CameraDirection.Back
                    }
                    // TODO: would be nicer to initialize the camera on the right device to begin with
                    if (defaultDirection != call.mediaManager.camera.direction.value) {
                        call.mediaManager.camera.flip()
                    }
                }

                // if there is no preview and the microphone hasn't been selected by the user fallback to settings
                if (call.mediaManager.microphone.status.value == DeviceStatus.NotSelected) {
                    val enabled = settings?.audio?.micDefaultOn == true
                    call.mediaManager.microphone.setEnabled(enabled)
                }

                timer.split("media enabled")
                // step 4 add the audio track to the publisher
                setLocalTrack(
                    TrackType.TRACK_TYPE_AUDIO,
                    AudioTrack(
                        streamId = buildTrackId(TrackType.TRACK_TYPE_AUDIO),
                        audio = call.mediaManager.audioTrack
                    )
                )
                publisher.addAudioTransceiver(
                    call.mediaManager.audioTrack,
                    listOf(buildTrackId(TrackType.TRACK_TYPE_AUDIO))
                )
                // step 5 create the video track
                setLocalTrack(
                    TrackType.TRACK_TYPE_VIDEO,
                    VideoTrack(
                        streamId = buildTrackId(TrackType.TRACK_TYPE_VIDEO),
                        video = call.mediaManager.videoTrack
                    )
                )
                // render it on the surface. but we need to start this before forwarding it to the publisher
                logger.v { "[createUserTracks] #sfu; videoTrack: ${call.mediaManager.videoTrack.stringify()}" }
                publisher.addVideoTransceiver(
                    call.mediaManager.videoTrack,
                    listOf(buildTrackId(TrackType.TRACK_TYPE_VIDEO))
                )
            }
        }

        // step 6 - onNegotiationNeeded will trigger and complete the setup using SetPublisherRequest
        timer.finish()
        listenToMediaChanges()

        // subscribe to the tracks of other participants
        updateParticipantSubscriptions(true)
        return
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
        isEnabled: Boolean
    ) {
        logger.d { "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, isEnabled: $isEnabled" }
        val track = getTrack(sessionId, trackType)
        track?.enableVideo(isEnabled)
        track?.enableAudio(isEnabled)
    }

    fun cleanup() {
        logger.i { "[cleanup] #sfu; no args" }
        supervisorJob.cancel()

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

        // disconnect the socket and clean it up
        sfuConnectionModule.sfuSocket.cleanup()
    }

    /**
     * TODO: Probably partially move this
     * - set the camera track enabled
     * - updateMuteStateRequest
     *
     */
    fun setMuteState(isEnabled: Boolean, trackType: TrackType) {
        logger.d { "[setMuteState] #sfu; $trackType isEnabled: $isEnabled" }

        coroutineScope.launch {

            val request = UpdateMuteStatesRequest(
                session_id = sessionId,
                mute_states = listOf(
                    TrackMuteState(
                        track_type = trackType,
                        muted = !isEnabled
                    )
                ),
            )
            updateMuteState(request).onSuccessSuspend {
            }.onError {
                // TODO: handle error better
                throw IllegalStateException(it.message)
            }
        }
    }

    @VisibleForTesting
    public fun createSubscriber(): StreamPeerConnection {
        logger.i { "[createSubscriber] #sfu" }
        return clientImpl.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = { addStream(it) }, // addTrack
            onIceCandidateRequest = ::sendIceCandidate
        )
    }

    private suspend fun getSubscriberSdp(): SessionDescription {
        subscriber?.let {
            it.connection.apply {
                addTransceiver(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                    RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
                    )
                )
                addTransceiver(
                    MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                    RtpTransceiver.RtpTransceiverInit(
                        RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
                    )
                )
            }

            val result = it.createOffer()

            return if (result is Success) {
                mangleSdp(result.value)
            } else {
                throw Error("Couldn't create a generic SDP, create offer failed")
            }
        } ?: throw Error("Couldn't create a generic SDP, subscriber isn't setup")
    }

    fun mangleSdp(sdp: SessionDescription): SessionDescription {
        val settings = call.state.settings.value
        val red = settings?.audio?.redundantCodingEnabled ?: true
        val opus = settings?.audio?.opusDtxEnabled ?: true

        return mangleSdpUtil(sdp, red, opus)
    }

    @VisibleForTesting
    fun createPublisher(): StreamPeerConnection? {
        val publisher = clientImpl.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::onNegotiationNeeded,
            onIceCandidateRequest = ::sendIceCandidate,
            maxPublishingBitrate = call.state.settings.value?.video?.targetResolution?.bitrate ?: 1_200_000
        )
        logger.i { "[createPublisher] #sfu; publisher: $publisher" }
        return publisher
    }

    private fun buildTrackId(trackTypeVideo: TrackType): String {
        // track prefix is only available after the join response
        val trackType = trackTypeVideo.value
        val trackPrefix = call.state?.me?.value?.trackLookupPrefix
        val old = "$trackPrefix:$trackType:${(Math.random() * 100).toInt()}"
        return old // UUID.randomUUID().toString()
    }

    /**
     * Change the quality of video we upload when the ChangePublishQualityEvent event is received
     * This is used for dynsacle
     */
    internal fun updatePublishQuality(event: ChangePublishQualityEvent) {
        if (publisher == null) {
            return
        }
        val enabledRids =
            event.changePublishQuality.video_senders.firstOrNull()?.layers?.associate { it.name to it.active }
        val transceiver = publisher?.videoTransceiver ?: return
        // enable or disable tracks
        val encodings = transceiver.sender.parameters.encodings.toList()
        for (encoding in encodings) {
            encoding.active = enabledRids?.get(encoding.rid ?: "") ?: false
        }

        logger.i { "video quality: marking layers active $enabledRids " }

        transceiver.sender.parameters.encodings.clear()
        transceiver.sender.parameters.encodings.addAll(encodings)

        // publisher?.videoTransceiver?.sender?.parameters = transceiver.sender.parameters

        return

        logger.v { "[updatePublishQuality] #sfu; updateQuality: $enabledRids" }
        val params = transceiver.sender.parameters

        var encodingChanged = false
        logger.v { "[updatePublishQuality] #sfu; currentQuality: $params" }

//        for (encoding in params.encodings) {
//            if (encoding.rid != null) {
//                val shouldEnable = encoding.rid in enabledRids
//
//                if (shouldEnable && encoding.active) {
//                    updatedEncodings.add(encoding)
//                } else if (!shouldEnable && !encoding.active) {
//                    updatedEncodings.add(encoding)
//                } else {
//                    encodingChanged = true
//                    encoding.active = shouldEnable
//                    updatedEncodings.add(encoding)
//                }
//            }
//        }
//        if (encodingChanged && false) {
// //            logger.v { "[updatePublishQuality] #sfu; updatedEncodings: $updatedEncodings" }
//            params.encodings.clear()
//            params.encodings.addAll(updatedEncodings)
//
//            publisher?.videoTransceiver?.sender?.parameters = params
//        }
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
                    user_id = participant.user.value.id,
                    track_type = TrackType.TRACK_TYPE_VIDEO,
                    dimension = VideoDimension(960, 720),
                    session_id = participant.sessionId
                )
                tracks.add(track)
            }
            if (participant.screenSharingEnabled.value) {
                val track = TrackSubscriptionDetails(
                    user_id = participant.user.value.id,
                    track_type = TrackType.TRACK_TYPE_SCREEN_SHARE,
                    dimension = VideoDimension(960, 720),
                    session_id = participant.sessionId
                )
                tracks.add(track)
            }
        }

        return tracks
    }

    internal fun visibleTracks(): List<TrackSubscriptionDetails> {
        val participants = call.state.remoteParticipants.value
        val trackDisplayResolution = trackDimensions.value

        var tracks = participants.map { participant ->
            val trackDisplay = trackDisplayResolution[participant.sessionId] ?: emptyMap()

            trackDisplay.entries.filter { it.value.visible }.map { display ->
                dynascaleLogger.i { "[visibleTracks] $sessionId subscribing ${participant.sessionId} to : ${display.key}" }
                TrackSubscriptionDetails(
                    user_id = participant.user.value.id,
                    track_type = display.key,
                    dimension = display.value.dimensions,
                    session_id = participant.sessionId
                )
            }
        }.flatten()
        return tracks
    }

    private fun updateParticipantSubscriptions(useDefaults: Boolean = false) {
        // default is to subscribe to the top 5 sorted participants
        val tracks = if (useDefaults) {
            defaultTracks()
        } else {
            // if we're not using the default, sub to visible tracks
            visibleTracks()
        }

        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            tracks = tracks
        )
        val sessionsIds = tracks.map { it.track_type to it.session_id }
        dynascaleLogger.i { "[updateParticipantsSubscriptions] $useDefaults #sfu; $sessionId subscribing to : $sessionsIds" }

        // can be empty if you're alone in a call
        if (tracks.isNotEmpty()) {
            // skip if nothing changed
            if (lastTracks == tracks) {
                dynascaleLogger.i { "[updateParticipantsSubscriptions] #sfu; tracks are the same, not updating" }
                return
            }
            coroutineScope.launch {
                when (val result = updateSubscriptions(request)) {
                    is Success -> {
                        dynascaleLogger.v { "[updateParticipantsSubscriptions] #sfu; succeed" }
                        lastTracks = tracks
                    }

                    is Failure -> {
                        // TODO: this breaks the call, we should handle this better
                        dynascaleLogger.e { "[updateParticipantsSubscriptions] #sfu; failed: $result" }
                    }
                }
            }
        }
    }

    fun handleEvent(event: VideoEvent) {
        logger.i { "[rtc handleEvent] #sfu; event: $event" }
        if (event is JoinCallResponseEvent) {
            logger.i { "[rtc handleEvent] joinEventResponse.value: $event" }

            joinEventResponse.value = event
        }
        if (event is SfuDataEvent) {
            coroutineScope.launch {
                logger.v { "[onRtcEvent] event: $event" }
                when (event) {
                    is ICETrickleEvent -> handleIceTrickle(event)
                    is SubscriberOfferEvent -> handleSubscriberOffer(event)
                    // this dynascale event tells the SDK to change the quality of the video it's uploading
                    is ChangePublishQualityEvent -> updatePublishQuality(event)

                    is TrackPublishedEvent -> {
                        updatePublishState(event.userId, event.sessionId, event.trackType, true)
                    }

                    is TrackUnpublishedEvent -> {
                        updatePublishState(event.userId, event.sessionId, event.trackType, false)
                    }

                    is ParticipantJoinedEvent -> {
                        // the UI layer will automatically trigger updateParticipantsSubscriptions
                    }

                    else -> {
                        logger.d { "[onRtcEvent] skipped event: $event" }
                    }
                }
            }
        }
    }

    /**
     Section, basic webrtc calls
     */

    /**
     * Whenever onIceCandidateRequest is called we send the ice candidate
     */
    private fun sendIceCandidate(candidate: IceCandidate, peerType: StreamPeerType) {
        coroutineScope.launch {
            logger.d { "[sendIceCandidate] #sfu; #${peerType.stringify()}; candidate: $candidate" }
            val iceTrickle = ICETrickle(
                peer_type = peerType.toPeerType(),
                ice_candidate = Json.encodeToString(candidate),
                session_id = sessionId
            )
            logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; iceTrickle: $iceTrickle" }
            val result = sendIceCandidate(iceTrickle)
            logger.v { "[sendIceCandidate] #sfu; #${peerType.stringify()}; completed: $result" }
        }
    }

    @VisibleForTesting
    /**
     * Triggered whenever we receive new ice candidate from the SFU
     */
    suspend fun handleIceTrickle(event: ICETrickleEvent) {
        logger.d { "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; candidate: ${event.candidate}" }
        val iceCandidate: IceCandidate = Json.decodeFromString(event.candidate)
        val result = if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED) {
            publisher?.addIceCandidate(iceCandidate)
        } else {
            subscriber?.addIceCandidate(iceCandidate)
        }
        logger.v { "[handleTrickle] #sfu; #${event.peerType.stringify()}; result: $result" }
    }

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

        val offerDescription = SessionDescription(
            SessionDescription.Type.OFFER, offerEvent.sdp
        )
        subscriber.setRemoteDescription(offerDescription)
        val answerResult = subscriber.createAnswer()
        if (answerResult !is Success) {
            logger.w { "[handleSubscriberOffer] #sfu; #subscriber; rejected (createAnswer failed): $answerResult" }
            return
        }
        val answerSdp = mangleSdp(answerResult.value)

        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; answerSdp: ${answerSdp.description}" }
        val setAnswerResult = subscriber.setLocalDescription(answerSdp)
        if (setAnswerResult !is Success) {
            logger.w { "[handleSubscriberOffer] #sfu; #subscriber; rejected (setAnswer failed): $setAnswerResult" }
            return
        }
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; setAnswerResult: $setAnswerResult" }
        val sendAnswerRequest = SendAnswerRequest(
            PeerType.PEER_TYPE_SUBSCRIBER, answerSdp.description, sessionId
        )
        val sendAnswerResult = sendAnswer(sendAnswerRequest)
        logger.v { "[handleSubscriberOffer] #sfu; #subscriber; sendAnswerResult: $sendAnswerResult" }
    }

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
     */
    @VisibleForTesting
    fun onNegotiationNeeded(
        peerConnection: StreamPeerConnection,
        peerType: StreamPeerType
    ) {
        val id = Random.nextInt().absoluteValue
        logger.d { "[negotiate] #$id; #sfu; #${peerType.stringify()}; peerConnection: $peerConnection" }
        coroutineScope.launch {
            peerConnection.createOffer().onSuccessSuspend { originalSDP ->
                val data = mangleSdp(originalSDP)
                logger.v { "[negotiate] #$id; #sfu; #${peerType.stringify()}; offerSdp: $data" }

                val result = peerConnection.setLocalDescription(data)
                if (result.isFailure) {
                    // TODO: better error handling
                    throw IllegalStateException(result.toString())
                }

                // the Sfu WS needs to be connected before calling SetPublisherRequest
                if (joinEventResponse.value == null) {
                    throw IllegalStateException("SFU WS isn't connected")
                }

                val captureResolution = call.camera.resolution.value

                val transceivers = peerConnection.connection.transceivers.toList()
                val trackInfos = transceivers.filter {
                    it.direction == RtpTransceiver.RtpTransceiverDirection.SEND_ONLY && it.sender?.track() != null
                }.map { transceiver ->
                    val track = transceiver.sender.track()!!

                    val trackType = when (track.kind()) {
                        "audio" -> TrackType.TRACK_TYPE_AUDIO
                        "screen" -> TrackType.TRACK_TYPE_SCREEN_SHARE
                        "video" -> TrackType.TRACK_TYPE_VIDEO
                        else -> TrackType.TRACK_TYPE_UNSPECIFIED
                    }

                    val layers: List<VideoLayer> = if (trackType != TrackType.TRACK_TYPE_VIDEO) {
                        emptyList()
                    } else {
                        // we tell the Sfu which resolutions we're sending
                        transceiver.sender.parameters.encodings.map {
                            val scaleDownFactor = mapOf("q" to 4, "h" to 2, "f" to 1)
                            val scale = scaleDownFactor[it.rid]
                            val width = captureResolution?.width?.div(scale!!) ?: 0
                            val height = scale?.let { it1 -> captureResolution?.height?.div(it1) }
                                ?: 0
                            VideoLayer(
                                rid = it.rid ?: "",
                                video_dimension = VideoDimension(
                                    width = width,
                                    height = height,
                                ),
                                bitrate = it.maxBitrateBps ?: 0,
                                fps = captureResolution?.framerate?.max ?: 0
                            )
                        }
                    }

                    TrackInfo(
                        track_id = track.id(),
                        track_type = trackType,
                        layers = layers
                    )
                }

                val request = SetPublisherRequest(
                    sdp = data.description,
                    session_id = sessionId,
                    tracks = trackInfos
                )
                val setPublisherResult = setPublisher(request)

                setPublisherResult.onSuccessSuspend {
                    if (it.error != null) {
                        throw IllegalStateException(it.error.toString())
                    }
                    logger.v { "[negotiate] #$id; #sfu; #${peerType.stringify()}; answerSdp: $it" }

                    val answerDescription = SessionDescription(
                        SessionDescription.Type.ANSWER, it.sdp
                    )

                    // set the remote peer connection, and handle queued ice candidates
                    peerConnection.setRemoteDescription(answerDescription)
                }.onError {
                    throw IllegalStateException("[negotiate] #$id; #sfu; #${peerType.stringify()}; failed: $it")
                    logger.e { "[negotiate] #$id; #sfu; #${peerType.stringify()}; failed: $it" }
                }
            }
        }
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the publisher exposes.
     */
    fun getPublisherStats(): StateFlow<RTCStatsReport?>? {
        return publisher?.getStats()
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the subscriber exposes.
     */
    fun getSubscriberStats(): StateFlow<RTCStatsReport?> {
        return subscriber?.getStats() ?: MutableStateFlow(null)
    }

    /***
     * Section, API endpoints
     */

    internal suspend fun <T : Any> wrapAPICall(apiCall: suspend () -> T): Result<T> {
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
                        e
                    )
                )
            }
        }
    }

    suspend fun parseError(e: Throwable): Failure {
        return Failure(
            io.getstream.result.Error.ThrowableError(
                "CallClientImpl error needs to be handled",
                e
            )
        )
    }

    // TODO: handle the .error field on the Response objects
    // reply to when we get an offer from the SFU
    suspend fun sendAnswer(request: SendAnswerRequest): Result<SendAnswerResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.signalService.sendAnswer(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // send whenever we have a new ice candidate
    suspend fun sendIceCandidate(request: ICETrickle): Result<ICETrickleResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.signalService.iceTrickle(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // call after onNegotiation Needed
    suspend fun setPublisher(request: SetPublisherRequest): Result<SetPublisherResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.signalService.setPublisher(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // share what size and which participants we're looking at
    suspend fun updateSubscriptions(request: UpdateSubscriptionsRequest): Result<UpdateSubscriptionsResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.signalService.updateSubscriptions(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    suspend fun updateMuteState(request: UpdateMuteStatesRequest): Result<UpdateMuteStatesResponse> =
        wrapAPICall {
            val result = sfuConnectionModule.signalService.updateMuteStates(request)
            result.error?.let {
                throw RtcException(error = it, message = it.message)
            }
            result
        }

    // sets display track visiblity
    @Synchronized
    fun updateTrackDimensions(sessionId: String, trackType: TrackType, visible: Boolean, dimensions: VideoDimension = VideoDimension(960, 720)) {
        // The map contains all track dimensions for all participants
        dynascaleLogger.i { "uuu23 $sessionId $trackType $visible $dimensions" }

        // first we make a copy of the dimensions
        val trackDimensionsMap = trackDimensions.value.toMutableMap()

        // next we get or create the dimensions for this participants
        var participantTrackDimensions = trackDimensionsMap[sessionId]?.toMutableMap() ?: mutableMapOf()

        // last we get the dimensions for this specific track type
        val oldTrack = participantTrackDimensions[trackType] ?: TrackDimensions(dimensions = dimensions, visible = visible)
        val newTrack = oldTrack.copy(visible = visible, dimensions = dimensions)
        participantTrackDimensions[trackType] = newTrack

        trackDimensionsMap[sessionId] = participantTrackDimensions

        // Updates are debounced
        dynascaleLogger.i { "updateTrackDimensions $trackDimensionsMap" }
        trackDimensions.value = trackDimensionsMap
    }

    suspend fun switchSfu(sfuUrl: String, sfuToken: String, remoteIceServers: List<IceServer>) {
        logger.i { "switchSfu from ${this.sfuUrl} to $sfuUrl" }
        val timer = clientImpl.debugInfo.trackTime("call.switchSfu")
        // update internal vars ot the new SFU
        this.sfuUrl = sfuUrl
        this.sfuToken = sfuToken
        this.remoteIceServers = remoteIceServers
        this.iceServers = buildRemoteIceServers(remoteIceServers)

        // create the new socket
        val getSdp = suspend {
            getSubscriberSdp().description
        }
        // TODO: updating the turn server requires a new peer connection
        sfuConnectionModule =
            connectionModule.createSFUConnectionModule(sfuUrl, sessionId, sfuToken, getSdp)
        listenToSocket()
        sfuConnectionModule.sfuSocket.connect()
        timer.split("socket connected")

        // ice restart
        reconnect()
        timer.finish("ice restart in progress")
    }
}
