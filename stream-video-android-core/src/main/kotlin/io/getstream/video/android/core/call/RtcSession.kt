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
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.*
import io.getstream.video.android.core.StreamVideoImpl
import io.getstream.video.android.core.call.connection.StreamPeerConnection
import io.getstream.video.android.core.call.signal.socket.SfuSocketListener
import io.getstream.video.android.core.call.state.ConnectionState
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.filter.InFilterObject
import io.getstream.video.android.core.filter.toMap
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.internal.module.SFUConnectionModule
import io.getstream.video.android.core.model.CallSettings
import io.getstream.video.android.core.model.IceCandidate
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.QueryMembersData
import io.getstream.video.android.core.model.StreamCallId
import io.getstream.video.android.core.model.StreamPeerType
import io.getstream.video.android.core.model.toPeerType
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.*
import io.getstream.video.android.core.utils.buildAudioConstraints
import io.getstream.video.android.core.utils.buildConnectionConfiguration
import io.getstream.video.android.core.utils.buildMediaConstraints
import io.getstream.video.android.core.utils.buildRemoteIceServers
import io.getstream.video.android.core.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import retrofit2.HttpException
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.JoinResponse
import stream.video.sfu.models.CallState
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.models.Participant
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
import java.util.*
import kotlin.Error
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * Alright, what does this do?
 *
 * - Holds the subscriber and publisher peer connection
 * - Connects to the SFU
 *
 * - Makes API calls to the SFU
 * - Camera & Rendering helpers
 *
 *
 * Video Tracks
 *
 *
 * WebRTC behaviour
 *
 *
 */
public class RtcSession internal constructor(
    private val client: StreamVideo,
    private val connectionModule: ConnectionModule,
    private val call: Call,
    private val SFUUrl: String,
    private val SFUToken: String,
    private val latencyResults: Map<String, List<Float>>,
    private val remoteIceServers: List<IceServer>,
) : SFUSession, SfuSocketListener {
    private val context = client.context
    private val logger by taggedLogger("Call:ActiveSFUSession")
    private val clientImpl = client as StreamVideoImpl
    private val scope = clientImpl.scope
    /** session id is generated client side */
    private val sessionId = UUID.randomUUID().toString()

    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    // run all calls on a supervisor job so we can easily cancel them
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(scope.coroutineContext + supervisorJob)

    // TODO:
    // so we need to update tracks for all participants
    // Maybe it would be cleaner to store tracks here
    // and point the participants at it?

    /**
     * Connection and WebRTC.
     */

    private val iceServers by lazy { buildRemoteIceServers(remoteIceServers) }

    private val connectionConfiguration: PeerConnection.RTCConfiguration by lazy {
        buildConnectionConfiguration(iceServers)
    }

    /** subscriber peer connection is used for subs */
    private var subscriber: StreamPeerConnection? = null
    /** publisher for publishing, using 2 peer connections prevents race conditions in the offer/answer cycle */
    @VisibleForTesting
    var publisher: StreamPeerConnection? = null


    private val mediaConstraints: MediaConstraints by lazy {
        buildMediaConstraints()
    }

    private val audioConstraints: MediaConstraints by lazy {
        buildAudioConstraints()
    }






    private var sfuConnectionModule: SFUConnectionModule

    init {
        val preferences = UserPreferencesManager.getPreferences()
        val user = preferences.getUserCredentials()
        if (preferences.getApiKey().isBlank() ||
            user?.id.isNullOrBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        sfuConnectionModule = connectionModule.createSFUConnectionModule(SFUUrl, SFUToken)

        sfuConnectionModule.sfuSocket.addListener(object: SfuSocketListener {
            public override fun onConnecting() {
                // TODO
            }

            public override fun onConnected(event: SFUConnectedEvent) {
                // TODO
            }

            public override fun onDisconnected(cause: DisconnectCause) {
                // TODO
            }

            public override fun onError(error: Error) {
                TODO()
            }

            public override fun onEvent(event: SfuDataEvent) {
                clientImpl.fireEvent(event, call.cid)
            }
        })
        sfuConnectionModule.sfuSocket.connectSocket()
        coroutineScope.launch {
            connectRTC()
            listenToMediaChanges()
        }
    }

    suspend fun listenToMediaChanges() {
        // update the tracks when the camera or microphone status changes
        call.mediaManager.camera.status.collectLatest {
            localTracks[TRACK_TYPE_VIDEO].setEnabled(it == DeviceStatus.Enabled)
        }

        call.mediaManager.camera.selectedDevice.collectLatest {
            // update the track with the new device
        }
    }

    suspend fun connectRTC() {
        // step 1 setup the peer connections
        createSubscriber()
        // if we are allowed to publish, create a peer connection for it
        // TODO: real settings check
        val publishing = true
        if (publishing) {
            createPublisher()
        }

        if (publishing ) {
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
                source = audioSource, trackId = buildTrackId(TRACK_TYPE_AUDIO)
            )
            audioTrack.setEnabled(true)
            publisher?.addAudioTransceiver(audioTrack, listOf(sessionId))
            // step 5 create the video track
            val videoTrack = clientImpl.peerConnectionFactory.makeVideoTrack(
                source = videoSource, trackId = buildTrackId(TRACK_TYPE_VIDEO)
            )
            // render it on the surface. but we need to start this before forwarding it to the publisher
            capturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            // TODO: understand how to start with only rendering on the surface view
            videoTrack.setEnabled(true)
            logger.v { "[createUserTracks] #sfu; videoTrack: ${videoTrack.stringify()}" }
            publisher?.addVideoTransceiver(localVideoTrack!!, listOf(sessionId))
        }

        // step 6 - execute the join request
        val result = executeJoinRequest()
        // step 7 - onNegotiationNeeded will trigger and complete the setup using SetPublisherRequest
        // step 8 - We will receive the JoinCallResponseEvent event

    }

    /**
     * updateLocalVideoTrack updates the local video track
     */
    internal fun updateLocalVideoTrack(localVideoTrack: org.webrtc.VideoTrack) {

        val videoTrack = io.getstream.video.android.core.model.VideoTrack(
            video = localVideoTrack,
            streamId = "${call.client.userId}:${localVideoTrack.id()}"
        )

        // TODO: update participant.videoTrack to videoTrack
    }

    /**
     * Responds ot TrackPublishedEvent event
     * @see TrackPublishedEvent
     * @see TrackUnpublishedEvent
     *
     * It gets the participant and updates the tracks
     */
    internal fun updatePublishState(
        userId: String,
        sessionId: String,
        trackType: TrackType,
        isEnabled: Boolean
    ) {

        logger.d { "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, isEnabled: $isEnabled" }

        val participant = requireParticipant(sessionId)

        val videoTrackSize = if (trackType == TrackType.TRACK_TYPE_VIDEO) {
            if (isEnabled) {
                participant.videoTrackSize
            } else {
                0 to 0
            }
        } else {
            participant.videoTrackSize
        }

        val screenShareTrack = if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
            if (isEnabled) {
                participant.screenSharingTrack
            } else {
                null
            }
        } else {
            participant.screenSharingTrack
        }

        val updated = participant.copy(
            videoTrackSize = videoTrackSize,
            screenSharingTrack = screenShareTrack,
            publishedTracks = if (isEnabled) participant.publishedTracks + trackType else participant.publishedTracks - trackType
        )

        updateParticipant(updated)

        if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE && !isEnabled) {
            _screenSharingSession.value = null
        }
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
    private var captureResolution: CameraEnumerationAndroid.CaptureFormat? = null

    override fun clear() {
        logger.i { "[clear] #sfu; no args" }
        supervisorJob.cancelChildren()

        connectionState = ConnectionState.DISCONNECTED
        sessionId = ""

        subscriber?.connection?.close()
        publisher?.connection?.close()
        subscriber = null
        publisher = null

        sfuConnectionModule.sfuSocket.releaseConnection()

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper.stopListening()

        isCapturingVideo = false
    }


    override fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            // If we are not connected to the SFU we do not want to send requests or initialise any tracks not to waste
            // resources if the user declines a call.
            // TODO
//            if (callEngine.callState.value !is StreamCallState.Connected) {
//                _isVideoEnabled.value = isEnabled
//                return@launch
//            }

            if (!isCapturingVideo && isEnabled) {
                mediaManager.startCapturingLocalVideo(CameraMetadata.LENS_FACING_FRONT)
            }
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
                setCameraEnabled(isEnabled)
                localVideoTrack?.setEnabled(isEnabled)
                _isVideoEnabled.value = isEnabled
            }
        }
    }

    override fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        coroutineScope.launch {
            // If we are not connected to the SFU we do not want to send requests or initialise any tracks not to waste
            // resources if the user declines a call.
            // TODO
//            if (callEngine.callState.value !is StreamCallState.Connected) {
//                _isAudioEnabled.value = isEnabled
//                return@launch
//            }

            setupAudioTrack()

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
                setMicrophoneEnabled(isEnabled)
                localAudioTrack?.setEnabled(isEnabled)
                _isAudioEnabled.value = isEnabled
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
            onStreamAdded = { call.state.addStream(it) }, // addTrack
            onIceCandidateRequest = ::sendIceCandidate
        )
        logger.i { "[createSubscriber] #sfu; subscriber: $subscriber" }
        return subscriber
    }



    private suspend fun executeJoinRequest(): Result<JoinResponse> {

        val sdp = mangleSDP(getGenericSdp())

        val request = JoinRequest(
            session_id = sessionId,
            token = "TODO: TOKEN",
            subscriber_sdp = sdp.description
        )
        logger.d { "[executeJoinRequest] request: $request" }

        return try {
            withTimeout(TIMEOUT) {
                val connected = isConnected.value
                logger.d { "[executeJoinRequest] is connected: $connected" }
                sfuConnectionModule.sfuSocket.sendJoinRequest(request)
                logger.d { "[executeJoinRequest] sfu join request is sent" }
                // TODO: callEngine.onSfuJoinSent(request)
                logger.d { "[executeJoinRequest] request is sent" }
                val event = sfuEvents.filterIsInstance<JoinCallResponseEvent>().first()
                logger.d { "[executeJoinRequest] completed: $event" }
                Success(JoinResponse(event.callState))
            }
        } catch (e: Throwable) {
            logger.e { "[executeJoinRequest] failed: $e" }
            Failure(
                Error.ThrowableError(e.message ?: "Couldn't execute a join request.", e)
            )
        }
    }

    private suspend fun getGenericSdp(): SessionDescription {

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
            result.value
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


    override fun onConnected(event: SFUConnectedEvent) {
        // trigger an event in the client as well for SFU events. makes it easier to subscribe
        println("SFU onconnected")
        clientImpl.fireEvent(event, call.cid)
        coroutineScope.launch {
            logger.i { "[onConnected] event: $event" }
            isConnected.value = true
        }
    }

    override fun onDisconnected(cause: DisconnectCause) {
        println("SFU onDisconnected")

        coroutineScope.launch {
            logger.i { "[onDisconnected] cause: $cause" }
            isConnected.value = false
        }
    }

    private fun buildTrackId(trackTypeVideo: String): String {
        return "${call.state?.me?.value?.trackLookupPrefix}:$trackTypeVideo:${(Math.random() * 100).toInt()}"
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
        if (event is SfuDataEvent) {
            coroutineScope.launch {
                logger.v { "[onRtcEvent] event: $event" }
                when (event) {
                    is ICETrickleEvent -> handleTrickle(event)
                    is SubscriberOfferEvent -> handleSubscriberOffer(event)
                    // this dynascale event tells the SDK to change the quality of the video it's uploading
                    is ChangePublishQualityEvent -> updatePublishQuality(event)

                    is TrackPublishedEvent -> {

                        call?.state?.updatePublishState(event.userId, event.sessionId, event.trackType, true)

                    }
                    is TrackUnpublishedEvent -> {

                        call?.state?.updatePublishState(event.userId, event.sessionId, event.trackType, false)
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
     * Triggered whenver we receive new ice candidate from the SFU
     */
    suspend fun handleTrickle(event: ICETrickleEvent) {
        logger.d { "[handleTrickle] #sfu; #${event.peerType.stringify()}; candidate: ${event.candidate}" }
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

                peerConnection.setLocalDescription(data)

                val trackInfos = peerConnection.connection.transceivers.filter {
                    it.direction == RtpTransceiver.RtpTransceiverDirection.SEND_ONLY && it.sender?.track() != null
                }.map { transceiver ->
                    val track = transceiver.sender.track()!!

                    val trackType = when (track.kind()) {
                        "audio" -> TrackType.TRACK_TYPE_AUDIO
                        "screen" -> TrackType.TRACK_TYPE_SCREEN_SHARE
                        "video" -> TrackType.TRACK_TYPE_VIDEO
                        else -> TrackType.TRACK_TYPE_UNSPECIFIED
                    }

                    val layers: List<VideoLayer> = transceiver.sender.parameters.encodings.map {
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

                setPublisher(request).onSuccessSuspend {
                    logger.v { "[negotiate] #$id; #sfu; #${peerType.stringify()}; answerSdp: $it" }

                    val answerDescription = SessionDescription(
                        SessionDescription.Type.ANSWER, it.sdp
                    )
                    peerConnection.setRemoteDescription(answerDescription)
                }.onError {
                    logger.e { "[negotiate] #$id; #sfu; #${peerType.stringify()}; failed: $it" }
                }
            }
        }
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the publisher exposes.
     */
    override fun getPublisherStats(): StateFlow<RTCStatsReport?> {
        return publisher?.getStats() ?: MutableStateFlow(null)
    }

    /**
     * @return [StateFlow] that holds [RTCStatsReport] that the subscriber exposes.
     */
    override fun getSubscriberStats(): StateFlow<RTCStatsReport?> {
        return subscriber?.getStats() ?: MutableStateFlow(null)
    }

    /***
     * Section, API endpoints
     */

    internal suspend fun <T : Any> wrapAPICall(apiCall: suspend () -> T): Result<T> {
        return withContext(scope.coroutineContext) {
            try {
                Success(apiCall())
            } catch (e: HttpException) {
                parseError(e)
            }
        }
    }

    suspend fun parseError(e: Throwable): Failure {
        return Failure(Error.ThrowableError("CallClientImpl error needs to be handled", e))
    }

    // reply to when we get an offer from the SFU
    suspend fun sendAnswer(request: SendAnswerRequest): Result<SendAnswerResponse> =
        wrapAPICall {
            sfuConnectionModule.signalService.sendAnswer(request)
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


    companion object {
        private const val TRACK_TYPE_VIDEO = "v"
        private const val TRACK_TYPE_AUDIO = "a"
        private const val TRACK_TYPE_SCREEN_SHARE = "s"
        private const val TIMEOUT = 30_000L
    }
}
