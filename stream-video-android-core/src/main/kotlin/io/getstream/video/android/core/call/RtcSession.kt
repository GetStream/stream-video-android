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

import android.hardware.camera2.CameraMetadata
import android.media.AudioAttributes.ALLOW_CAPTURE_BY_ALL
import android.media.AudioManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.DeviceStatus
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import io.getstream.video.android.core.call.state.ConnectionState
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.TrackWrapper
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildMediaConstraints
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.mangleSDP
import io.getstream.video.android.core.utils.mapState
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openapitools.client.models.VideoEvent
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
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
    private val sfuUrl: String,
    private val sfuToken: String,
    private val latencyResults: Map<String, List<Float>>,
    private val remoteIceServers: List<IceServer>,
) {
    private val context = client.context
    private val logger by taggedLogger("Call:RtcSession")
    private val clientImpl = client as StreamVideoImpl
    private val scope = clientImpl.scope

    private val sessionId = clientImpl.sessionId

    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED

    // run all calls on a supervisor job so we can easily cancel them
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(scope.coroutineContext + supervisorJob)

    /**
     * We can't publish tracks till we've received the join event response
     */
    private val joinEventResponse: MutableStateFlow<JoinCallResponseEvent?> = MutableStateFlow(null)

    // participants by session id -> participant state
    private val trackPrefixToSessionIdMap =
        call.state.participants.mapState { it.associate { it.trackLookupPrefix to it.sessionId } }

    // We need to update tracks for all participants
    // It's cleaner to store here and have the participant state reference to it
    var tracks: MutableMap<String, MutableMap<TrackType, TrackWrapper>> = mutableMapOf()

    fun getTrack(sessionId: String, type: TrackType): TrackWrapper? {
        if (!tracks.containsKey(sessionId)) {
            tracks[sessionId] = mutableMapOf()
        }
        return tracks[sessionId]?.get(type)
    }

    fun setTrack(sessionId: String, type: TrackType, track: TrackWrapper) {
        if (!tracks.containsKey(sessionId)) {
            tracks[sessionId] = mutableMapOf()
        }
        tracks[sessionId]?.set(type, track)
    }

    fun getLocalTrack(type: TrackType): TrackWrapper? {
        return getTrack(sessionId, type)
    }

    fun setLocalTrack(type: TrackType, track: TrackWrapper) {
        return setTrack(sessionId, type, track)
    }

    /**
     * Connection and WebRTC.
     */

    private val iceServers by lazy { buildRemoteIceServers(remoteIceServers) }

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

    private var sfuConnectionModule: SfuConnectionModule

    init {
        val preferences = UserPreferencesManager.getPreferences()
        val user = preferences.getUserCredentials()
        if (preferences.getApiKey().isBlank() ||
            user?.id.isNullOrBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        // step 1 setup the peer connections
        createSubscriber()
        val getSdp = suspend {
            getSubscriberSdp().description
        }
        sfuConnectionModule = connectionModule.createSFUConnectionModule(sfuUrl, sessionId, sfuToken, getSdp)
        // listen to socket events and errors
        scope.launch {
            sfuConnectionModule.sfuSocket.events.collect() {
                clientImpl.fireEvent(it, call.cid)
            }
        }
        scope.launch {
            sfuConnectionModule.sfuSocket.errors.collect() {
                if (clientImpl.developmentMode) {
                    throw it
                } else {
                    logger.e(it) { "permanent failure on socket connection" }
                }
            }
        }
    }

    suspend fun connect() {
        connectWs()
        // ensure that the join event has been handled before starting RTC
        joinEventResponse.first { it != null }
        connectRtc()
    }

    suspend fun connectWs() {
        sfuConnectionModule.sfuSocket.connect()
    }

    suspend fun listenToMediaChanges() {
        coroutineScope.launch {
            // update the tracks when the camera or microphone status changes
            call.mediaManager.camera.status.collectLatest {
                val track = getTrack(sessionId, TrackType.TRACK_TYPE_VIDEO)
                track?.video?.setEnabled(it == DeviceStatus.Enabled)
            }

            call.mediaManager.camera.selectedDevice.collectLatest {
                // update the track with the new device
            }
        }
    }

    /**
     * A single media stream contains multiple tracks. We receive it from the subcriber peer connection
     *
     * Loop over the audio and video tracks
     * Update the local tracks
     */
    internal fun addStream(mediaStream: MediaStream) {

        val (trackPrefix, trackTypeString) = mediaStream.id.split(':')
        val sessionId = trackPrefixToSessionIdMap.value[trackPrefix]!!
        val trackType = TrackType.fromValue(trackTypeString.toInt()) ?: throw IllegalStateException(
            "unrecognized track type"
        )

        logger.i { "[] #sfu; mediaStream: $mediaStream" }
        mediaStream.audioTracks.forEach { track ->
            logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
            track.setEnabled(true)
            val wrappedTrack = TrackWrapper(mediaStream.id, audio = track)
            setTrack(sessionId, trackType, wrappedTrack)
        }

        if (trackPrefixToSessionIdMap.value[trackPrefix].isNullOrEmpty()) {
            logger.w { "[addStream] skipping unrecognized trackPrefix $trackPrefix" }
            return
        }

        mediaStream.videoTracks.forEach { track ->
            track.setEnabled(true)
            val wrappedTrack = TrackWrapper(
                streamId = mediaStream.id,
                video = track
            )
            setTrack(sessionId, trackType, wrappedTrack)
        }
    }

    suspend fun connectRtc() {

        // if we are allowed to publish, create a peer connection for it
        // TODO: real settings check
        val publishing = true
        if (publishing) {
            createPublisher()
        }

        if (publishing) {
            // step 2 ensure all tracks are setup correctly
            // start capturing the video
            val manager = context.getSystemService<AudioManager>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                manager?.allowedCapturePolicy = ALLOW_CAPTURE_BY_ALL
            }
            call.mediaManager.camera.startCapture()
            call.mediaManager.microphone.startCapture()
            // step 3 tracks for video and audio
            val videoSource = clientImpl.peerConnectionFactory.makeVideoSource(false)
            val audioSource = clientImpl.peerConnectionFactory.makeAudioSource(audioConstraints)

            // step 4 add the audio track to the publisher
            val audioTrack = clientImpl.peerConnectionFactory.makeAudioTrack(
                source = audioSource, trackId = buildTrackId(TrackType.TRACK_TYPE_AUDIO)
            )
            audioTrack.setEnabled(true)
            setLocalTrack(TrackType.TRACK_TYPE_AUDIO, TrackWrapper(streamId=buildTrackId(TrackType.TRACK_TYPE_AUDIO), audio=audioTrack))

            publisher?.addAudioTransceiver(audioTrack, listOf(sessionId))
            // step 5 create the video track
            val videoTrack = clientImpl.peerConnectionFactory.makeVideoTrack(
                source = videoSource, trackId = buildTrackId(TrackType.TRACK_TYPE_VIDEO)
            )
            setLocalTrack(TrackType.TRACK_TYPE_VIDEO, TrackWrapper(streamId=buildTrackId(TrackType.TRACK_TYPE_VIDEO), video=videoTrack))
            // render it on the surface. but we need to start this before forwarding it to the publisher
            // TODO: clean this up, would be better to have some sensible API for this
            call.mediaManager.videoCapturer?.initialize(
                surfaceTextureHelper,
                context,
                videoSource.capturerObserver
            )
            // TODO: understand how to start with only rendering on the surface view
            videoTrack.setEnabled(true)
            logger.v { "[createUserTracks] #sfu; videoTrack: ${videoTrack.stringify()}" }
            publisher?.addVideoTransceiver(videoTrack!!, listOf(sessionId))
            setCameraEnabled(true)
            setMicrophoneEnabled(true)
        }

        // step 6 - onNegotiationNeeded will trigger and complete the setup using SetPublisherRequest

        listenToMediaChanges()
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
        track?.video?.setEnabled(isEnabled)
        track?.audio?.setEnabled(isEnabled)
    }

    /**
     * Video track helpers.
     */

    private val surfaceTextureHelper by lazy {
        SurfaceTextureHelper.create(
            "CaptureThread", clientImpl.peerConnectionFactory.eglBase.eglBaseContext
        )
    }

    private var videoCapturer: VideoCapturer? = null
    private var isCapturingVideo: Boolean = false
    // TODO: nicer way to monitor this
    private val captureResolution by lazy { call.mediaManager.captureResolution }

    fun clear() {
        logger.i { "[clear] #sfu; no args" }
        //supervisorJob.cancelChildren()

        connectionState = ConnectionState.DISCONNECTED

        subscriber?.connection?.close()
        publisher?.connection?.close()
        subscriber = null
        publisher = null

        sfuConnectionModule.sfuSocket.disconnect()

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper.stopListening()

        isCapturingVideo = false
    }

    /**
     * TODO: Probably partially move this
     * - set the camera track enabled
     * - updateMuteStateRequest
     *
     */
    fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }

        coroutineScope.launch {

            val request = UpdateMuteStatesRequest(
                session_id = sessionId,
                mute_states = listOf(
                    TrackMuteState(
                        track_type = TrackType.TRACK_TYPE_VIDEO,
                        muted = !isEnabled
                    )
                ),
            )
            updateMuteState(request).onSuccessSuspend {
            }
        }
    }

    fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            val request = UpdateMuteStatesRequest(
                session_id = sessionId,
                mute_states = listOf(
                    TrackMuteState(
                        track_type = TrackType.TRACK_TYPE_AUDIO,
                        muted = !isEnabled
                    )
                ),
            )

            updateMuteState(request).onSuccessSuspend {
            }
        }
    }

    @VisibleForTesting
    public fun createSubscriber(): StreamPeerConnection? {
        subscriber = clientImpl.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.SUBSCRIBER,
            mediaConstraints = mediaConstraints,
            onStreamAdded = { addStream(it) }, // addTrack
            onIceCandidateRequest = ::sendIceCandidate
        )
        logger.i { "[createSubscriber] #sfu; subscriber: $subscriber" }
        return subscriber
    }

    private suspend fun getSubscriberSdp(): SessionDescription {

        subscriber!!.connection.apply {
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

        val result = subscriber!!.createOffer()

        return if (result is Success) {
            mangleSDP(result.value)
        } else {
            throw Error("Couldn't create a generic SDP")
        }
    }

    @VisibleForTesting
    fun createPublisher(): StreamPeerConnection? {
        publisher = clientImpl.peerConnectionFactory.makePeerConnection(
            coroutineScope = coroutineScope,
            configuration = connectionConfiguration,
            type = StreamPeerType.PUBLISHER,
            mediaConstraints = MediaConstraints(),
            onNegotiationNeeded = ::onNegotiationNeeded,
            onIceCandidateRequest = ::sendIceCandidate,
        )
        logger.i { "[createPublisher] #sfu; publisher: $publisher" }
        return publisher
    }

    private fun buildTrackId(trackTypeVideo: TrackType): String {
        // track prefix is only available after the join response
        val trackType = trackTypeVideo.toString()
        val trackPrefix = call.state?.me?.value?.trackLookupPrefix
        return "$trackPrefix:$trackType:${(Math.random() * 100).toInt()}"
    }

    /**
     * Change the quality of video we upload when the ChangePublishQualityEvent event is received
     * This is used for dynsacle
     */
    private fun updatePublishQuality(event: ChangePublishQualityEvent) {
        val transceiver = publisher?.videoTransceiver ?: return

        val enabledRids =
            event.changePublishQuality.video_senders.firstOrNull()?.layers?.filter { it.active }
                ?.map { it.name } ?: emptyList()

        logger.v { "[updatePublishQuality] #sfu; updateQuality: $enabledRids" }
        val params = transceiver.sender.parameters

        val updatedEncodings = mutableListOf<RtpParameters.Encoding>()

        var encodingChanged = false
        logger.v { "[updatePublishQuality] #sfu; currentQuality: $params" }

        for (encoding in params.encodings) {
            if (encoding.rid != null) {
                val shouldEnable = encoding.rid in enabledRids

                if (shouldEnable && encoding.active) {
                    updatedEncodings.add(encoding)
                } else if (!shouldEnable && !encoding.active) {
                    updatedEncodings.add(encoding)
                } else {
                    encodingChanged = true
                    encoding.active = shouldEnable
                    updatedEncodings.add(encoding)
                }
            }
        }
        if (encodingChanged) {
            logger.v { "[updatePublishQuality] #sfu; updatedEncodings: $updatedEncodings" }
            params.encodings.clear()
            params.encodings.addAll(updatedEncodings)

            publisher?.videoTransceiver?.sender?.parameters = params
        }
    }

    /**
     * This is called when you are look at a different set of participants
     * or at a different size
     *
     * It tells the SFU that we want to receive person a's video at 1080p, and person b at 360p
     *
     * TODO: right now this is called by the SFU session, it should be called by the viewmodel
     * Since the viewmodel knows what's actually displayed
     */
    private fun updateParticipantsSubscriptions(participants: List<ParticipantState>) {
        val subscriptions = mutableMapOf<ParticipantState, VideoDimension>()
        val userId = client.user.id

        for (participant in participants) {
            val user = participant.user.value
            if (user.id != userId) {
                logger.d { "[updateParticipantsSubscriptions] #sfu; user.id: ${user.id}" }

                val dimension = VideoDimension(
                    width = participant.videoTrackSize.first,
                    height = participant.videoTrackSize.second
                )
                logger.d { "[updateParticipantsSubscriptions] #sfu; user.id: ${user.id}, dimension: $dimension" }
                subscriptions[participant] = dimension
            }
        }
        if (subscriptions.isEmpty()) {
            return
        }

        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            tracks = subscriptions.flatMap { (participant, videoDimensions) ->
                val user = participant.user.value
                listOf(
                    TrackSubscriptionDetails(
                        user_id = user.id,
                        track_type = TrackType.TRACK_TYPE_VIDEO,
                        dimension = videoDimensions,
                        session_id = participant.sessionId
                    ),
                    TrackSubscriptionDetails(
                        user_id = user.id,
                        track_type = TrackType.TRACK_TYPE_SCREEN_SHARE,
                        dimension = videoDimensions,
                        session_id = participant.sessionId
                    ),
                    TrackSubscriptionDetails(
                        user_id = user.id,
                        track_type = TrackType.TRACK_TYPE_AUDIO,
                        dimension = null,
                        session_id = participant.sessionId
                    )
                )
            }
        )
        logger.d { "[updateParticipantsSubscriptions] #sfu; request: $request" }

        coroutineScope.launch {
            when (val result = updateSubscriptions(request)) {
                is Success -> {
                    logger.v { "[updateParticipantsSubscriptions] #sfu; succeed" }
                }

                is Result.Failure -> {
                    logger.e { "[updateParticipantsSubscriptions] #sfu; failed: $result" }
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
            scope.launch {
                logger.v { "[onRtcEvent] event: $event" }
                when (event) {
                    is ICETrickleEvent -> handleIceTrickle(event)
                    is SubscriberOfferEvent -> handleSubscriberOffer(event)
                    is PublisherAnswerEvent -> TODO()
                    // this dynascale event tells the SDK to change the quality of the video it's uploading
                    is ChangePublishQualityEvent -> updatePublishQuality(event)

                    is TrackPublishedEvent -> {
                        updatePublishState(event.userId, event.sessionId, event.trackType, true)
                    }

                    is TrackUnpublishedEvent -> {
                        updatePublishState(event.userId, event.sessionId, event.trackType, false)
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
        val answerSdp = mangleSDP(answerResult.value)

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
                val data = mangleSDP(originalSDP, true, enableDtx = true)
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

                val transceivers = peerConnection.connection.transceivers
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
                        transceiver.sender.parameters.encodings.map {
                            VideoLayer(
                                rid = it.rid ?: "",
                                video_dimension = VideoDimension(
                                    width = captureResolution?.width ?: 0,
                                    height = captureResolution?.height ?: 0
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
        return withContext(scope.coroutineContext) {
            try {
                val result = apiCall()
                Success(result)
            } catch (e: HttpException) {
                parseError(e)
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
            result
        }

    // send whenever we have a new ice candidate
    suspend fun sendIceCandidate(request: ICETrickle): Result<ICETrickleResponse> =
        wrapAPICall { sfuConnectionModule.signalService.iceTrickle(request) }

    // call after onNegotiation Needed
    suspend fun setPublisher(request: SetPublisherRequest): Result<SetPublisherResponse> =
        wrapAPICall { sfuConnectionModule.signalService.setPublisher(request) }

    // share what size and which participants we're looking at
    suspend fun updateSubscriptions(request: UpdateSubscriptionsRequest): Result<UpdateSubscriptionsResponse> =
        wrapAPICall { sfuConnectionModule.signalService.updateSubscriptions(request) }

    suspend fun updateMuteState(muteStateRequest: UpdateMuteStatesRequest): Result<UpdateMuteStatesResponse> =
        wrapAPICall { sfuConnectionModule.signalService.updateMuteStates(muteStateRequest) }
}
