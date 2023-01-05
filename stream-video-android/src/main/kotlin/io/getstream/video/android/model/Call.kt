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

package io.getstream.video.android.model

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.os.Build
import android.view.View
import androidx.core.content.getSystemService
import io.getstream.log.taggedLogger
import io.getstream.video.android.audio.AudioHandler
import io.getstream.video.android.audio.AudioSwitchHandler
import io.getstream.video.android.call.utils.stringify
import io.getstream.video.android.events.AudioLevelChangedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.ui.TextureViewRenderer
import io.getstream.video.android.utils.updateValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.models.TrackType

public class Call(
    private val context: Context,
    private val getCurrentUserId: () -> String,
    private val eglBase: EglBase,
) {

    private val logger by taggedLogger("Call:Room")

    internal val audioHandler: AudioHandler by lazy {
        AudioSwitchHandler(context)
    }

    private val _callParticipants: MutableStateFlow<List<CallParticipantState>> =
        MutableStateFlow(emptyList())

    public val callParticipants: StateFlow<List<CallParticipantState>> = _callParticipants

    private val _localParticipant: MutableStateFlow<CallParticipantState?> = MutableStateFlow(null)

    public val localParticipant: Flow<CallParticipantState> =
        _localParticipant.filterNotNull()

    public val activeSpeakers: Flow<List<CallParticipantState>> = callParticipants.map { list ->
        list.filter { participant -> participant.hasAudio && participant.audioLevel > 0f }
    }

    private var lastPrimarySpeaker: CallParticipantState? = null

    private val _primarySpeaker: Flow<CallParticipantState?> =
        callParticipants.combine(activeSpeakers) { participants, speakers ->
            selectPrimaryParticipant(participants, speakers)
        }.onEach {
            lastPrimarySpeaker = it
        }

    public val primarySpeaker: Flow<CallParticipantState?> = _primarySpeaker

    private val _screenSharingSessions: MutableStateFlow<List<ScreenSharingSession>> =
        MutableStateFlow(emptyList())

    public val screenSharingSessions: Flow<List<ScreenSharingSession>> = _screenSharingSessions

    public val isScreenSharingActive: Boolean
        get() = _screenSharingSessions.value.isNotEmpty()

    public val localParticipantId: String
        get() = getCurrentUserId()

    public val localParticipantIdPrefix: String
        get() = _localParticipant.value?.idPrefix ?: ""

    private val audioManager by lazy {
        context.getSystemService<AudioManager>()
    }

    private val streamsToProcess: MutableList<MediaStream> = mutableListOf()

    public var onStreamAdded: (MediaStream) -> Unit = {}
    public var onStreamRemoved: (MediaStream) -> Unit = {}

    /**
     * Public API.
     */
    public fun initRenderer(
        videoRenderer: TextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        onRender: (View) -> Unit = {}
    ) {
        logger.d { "[initRenderer] #sfu; sessionId: $sessionId" }
        videoRenderer.init(
            eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    logger.v { "[initRenderer.onFirstFrameRendered] #sfu; sessionId: $sessionId" }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        updateParticipantTrackSize(
                            sessionId,
                            videoRenderer.measuredWidth,
                            videoRenderer.measuredHeight
                        )
                    }
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.v { "[initRenderer.onFrameResolutionChanged] #sfu; sessionId: $sessionId" }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        updateParticipantTrackSize(
                            sessionId,
                            videoRenderer.measuredWidth,
                            videoRenderer.measuredHeight
                        )
                    }
                }
            }
        )
    }

    public fun updateParticipantTrackSize(
        sessionId: String,
        measuredWidth: Int,
        measuredHeight: Int
    ) {
        logger.v { "[updateParticipantTrackSize] SessionId: $sessionId, width:$measuredWidth, height:$measuredHeight" }
        val oldState = _callParticipants.value

        val newState = oldState.updateValue(
            predicate = { it.sessionId == sessionId },
            transformer = { it.copy(videoTrackSize = measuredWidth to measuredHeight) }
        )

        _callParticipants.value = newState
    }

    internal fun addStream(mediaStream: MediaStream) {
        val participants = _callParticipants.value
        if (participants.isEmpty()) {
            streamsToProcess.add(mediaStream)
            return
        }

        logger.i { "[] #sfu; mediaStream: $mediaStream" }
        if (mediaStream.audioTracks.isNotEmpty()) {
            mediaStream.audioTracks.forEach { track ->
                logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
                track.setEnabled(true)
            }
        }

        if (mediaStream.videoTracks.isEmpty()) {
            onStreamAdded(mediaStream)
            return
        }

        var screenSharingSession: ScreenSharingSession? = null

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.idPrefix in mediaStream.id },
            transformer = {
                val track = replaceTrackIfNeeded(mediaStream, it.videoTrack?.streamId)

                if (track != null) {
                    logger.d { "[addStream] updating users with track $track" }
                    track.video.setEnabled(true)

                    val streamId = mediaStream.id
                    val videoTrack =
                        if (TrackType.TRACK_TYPE_VIDEO.name in streamId) track else it.videoTrack
                    val screenShareTrack =
                        if (TrackType.TRACK_TYPE_SCREEN_SHARE.name in streamId) track else it.screenSharingTrack

                    val tracks = it.publishedTracks.toMutableSet()

                    if (videoTrack != null) {
                        tracks.add(TrackType.TRACK_TYPE_VIDEO)
                    }

                    if (screenShareTrack != null) {
                        tracks.add(TrackType.TRACK_TYPE_SCREEN_SHARE)
                    }

                    val updatedParticipant = it.copy(
                        videoTrack = videoTrack,
                        screenSharingTrack = screenShareTrack,
                        publishedTracks = tracks
                    )

                    if (screenShareTrack != null) {
                        screenSharingSession = ScreenSharingSession(
                            track = screenShareTrack,
                            participant = updatedParticipant
                        )
                    }

                    updatedParticipant
                } else {
                    it
                }
            }
        )

        logger.d { "[addStream] updated list $updatedList" }
        screenSharingSession?.let { addScreenSharingSession(it) }
        _callParticipants.value = updatedList
        onStreamAdded(mediaStream)
    }

    private fun replaceTrackIfNeeded(mediaStream: MediaStream, streamId: String?): VideoTrack? {
        return if (streamId == null || streamId != mediaStream.id) {
            mediaStream.videoTracks?.firstOrNull()?.let { track ->
                VideoTrack(
                    streamId = mediaStream.id,
                    video = track
                )
            }
        } else {
            null
        }
    }

    private fun addScreenSharingSession(screenSharingSession: ScreenSharingSession) {
        logger.d { "[addScreenSharingSession] session: $screenSharingSession" }
        val list = _screenSharingSessions.value.toMutableList()
        val updated = list + screenSharingSession

        _screenSharingSessions.value = updated.distinctBy { it.track.streamId }
    }

    private fun removeScreenShareSession(sessionId: String) {
        logger.d { "[removeScreenShareSession] Session ID: $sessionId" }
        val list = _screenSharingSessions.value.toMutableList()
        val updated = list.filter { it.participant.sessionId != sessionId }

        _screenSharingSessions.value = updated
    }

    internal fun setParticipants(participants: List<CallParticipantState>) {
        this._callParticipants.value = participants
        logger.d { "[setParticipants] #sfu; allParticipants: ${_callParticipants.value}" }

        this._localParticipant.value = participants.firstOrNull { it.isLocal }
        logger.v { "[setParticipants] #sfu; localParticipants: ${_localParticipant.value}" }

        if (streamsToProcess.isNotEmpty()) {
            logger.v { "[setParticipants] #sfu; Adding streams to participants $streamsToProcess" }
            streamsToProcess.forEach(::addStream)
            streamsToProcess.clear()

            logger.v { "[setParticipants] #sfu; Added all streams to participants $streamsToProcess" }
        }
    }

    internal fun updateMuteState(
        userId: String,
        sessionId: String,
        trackType: TrackType,
        isEnabled: Boolean
    ) {
        logger.d { "[updateMuteState] #sfu; userId: $userId, sessionId: $sessionId, isEnabled: $isEnabled" }
        val currentParticipants = _callParticipants.value

        val updatedList = currentParticipants.updateValue(
            predicate = { it.sessionId == sessionId },
            transformer = {
                val videoTrackSize = if (trackType == TrackType.TRACK_TYPE_VIDEO) {
                    if (isEnabled) {
                        it.videoTrackSize
                    } else {
                        0 to 0
                    }
                } else {
                    it.videoTrackSize
                }

                val screenShareTrack = if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE) {
                    if (isEnabled) {
                        it.screenSharingTrack
                    } else {
                        null
                    }
                } else {
                    it.screenSharingTrack
                }

                it.copy(
                    videoTrackSize = videoTrackSize,
                    screenSharingTrack = screenShareTrack,
                    publishedTracks = if (isEnabled) it.publishedTracks + trackType else it.publishedTracks - trackType
                )
            }
        )

        _callParticipants.value = updatedList
        if (trackType == TrackType.TRACK_TYPE_SCREEN_SHARE && !isEnabled) {
            removeScreenShareSession(sessionId)
        }
    }

    internal fun addParticipant(participant: CallParticipantState) {
        logger.d { "[addParticipant] #sfu; participant: $participant" }
        _callParticipants.value = _callParticipants.value + participant
    }

    internal fun removeParticipant(event: ParticipantLeftEvent) {
        logger.d { "[removeParticipant] #sfu; event: $event" }
        val userId = event.participant.user_id

        _callParticipants.value = _callParticipants.value.filter { it.id != userId }
    }

    private fun selectPrimaryParticipant(
        participantsList: List<CallParticipantState>,
        speakers: List<CallParticipantState>,
    ): CallParticipantState? {
        var speaker = lastPrimarySpeaker
        val localParticipant = participantsList.firstOrNull { it.isLocal }

        if (speaker?.isLocal == true) {
            val remoteSpeaker =
                participantsList.filter { !it.isLocal }.maxByOrNull { it.audioLevel }

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        if (!participantsList.contains(speaker)) {
            speaker = participantsList.filter { !it.isLocal }.maxByOrNull { it.audioLevel }
                ?: localParticipant
        }

        if (speakers.isNotEmpty() && !speakers.contains(speaker)) {
            val remoteSpeaker =
                participantsList.filter { !it.isLocal }.maxByOrNull { it.audioLevel }

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        return speaker
    }

    internal fun updateLocalVideoTrack(localVideoTrack: org.webrtc.VideoTrack) {
        logger.d { "[updateLocalVideoTrack] #sfu; localVideoTrack: $localVideoTrack" }
        val localParticipant = _localParticipant.value ?: return
        val allParticipants = callParticipants.value

        val videoTrack = VideoTrack(
            video = localVideoTrack,
            streamId = "${getCurrentUserId()}:${localVideoTrack.id()}"
        )

        val updatedParticipant = localParticipant.copy(
            videoTrack = videoTrack
        )

        val updated = allParticipants.updateValue(
            predicate = { it.id == getCurrentUserId() },
            transformer = {
                it.copy(videoTrack = videoTrack)
            }
        )

        _localParticipant.value = updatedParticipant
        _callParticipants.value = updated
    }

    internal fun setupAudio(callSettings: CallSettings) {
        logger.d { "[setupAudio] #sfu; no args" }
        audioHandler.start()
        audioManager?.mode = MODE_IN_COMMUNICATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager?.availableCommunicationDevices ?: return
            val deviceType = if (callSettings.speakerOn) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }

            val device = devices.firstOrNull { it.type == deviceType } ?: return

            val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
            logger.d { "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" }
        }
    }

    internal fun disconnect() {
        logger.i { "[disconnect] #sfu; no args" }
        audioHandler.stop()
        val participants = _callParticipants.value
        _callParticipants.value = emptyList()

        participants.forEach {
            val track = it.videoTrack
            it.videoTrack = null
            track?.video?.dispose()
        }
    }

    public fun getLocalParticipant(): CallParticipantState? {
        return _callParticipants.value.firstOrNull { it.isLocal }
    }

    public fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _localParticipant.value ?: return
        val track = TrackType.TRACK_TYPE_VIDEO
        val tracks = localParticipant.publishedTracks

        val newTracks = if (isEnabled) tracks + track else tracks - track
        val updatedLocal = localParticipant.copy(
            publishedTracks = newTracks
        )
        _localParticipant.value = updatedLocal

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id == getCurrentUserId() },
            transformer = { it.copy(publishedTracks = newTracks) }
        )

        _callParticipants.value = updatedList
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _localParticipant.value ?: return
        val track = TrackType.TRACK_TYPE_AUDIO
        val tracks = localParticipant.publishedTracks

        val newTracks = if (isEnabled) tracks + track else tracks - track
        val updatedLocal = localParticipant.copy(publishedTracks = newTracks)
        _localParticipant.value = updatedLocal

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id == getCurrentUserId() },
            transformer = { it.copy(publishedTracks = newTracks) }
        )

        _callParticipants.value = updatedList
    }

    internal fun updateAudioLevel(event: AudioLevelChangedEvent) {
        logger.d { "[updateAudioLevel] #sfu; isEnabled: $event" }

        val current = _callParticipants.value.toMutableList()

        event.levels.forEach { (userId, level) ->
            val index = current.indexOfFirst { it.id == userId }

            if (index != -1) {
                val updatedUser = current[index].copy(
                    audioLevel = level.audioLevel,
                    isSpeaking = level.isSpeaking
                )
                current.removeAt(index)
                current.add(index, updatedUser)
            }
        }

        _callParticipants.value = current.sortedByDescending { it.audioLevel }
    }

    /**
     * Updates the information on the connection quality for each participant.
     *
     * @param updates A [List] of [ConnectionQualityInfo] containing the quality of each
     * participant's connection.
     */
    internal fun updateConnectionQuality(updates: List<ConnectionQualityInfo>) {
        val qualityMap = updates.associateBy { it.session_id }

        val current = _callParticipants.value
        val updated = current.map { user ->
            val qualityInfo = qualityMap[user.sessionId]

            if (qualityInfo != null) {
                user.copy(connectionQuality = qualityInfo.connection_quality)
            } else {
                user
            }
        }

        _callParticipants.value = updated
    }
}
