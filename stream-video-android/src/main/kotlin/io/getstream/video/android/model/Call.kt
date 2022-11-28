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
import io.getstream.logging.StreamLog
import io.getstream.video.android.audio.AudioHandler
import io.getstream.video.android.audio.AudioSwitchHandler
import io.getstream.video.android.call.utils.stringify
import io.getstream.video.android.events.AudioLevelChangedEvent
import io.getstream.video.android.events.MuteStateChangeEvent
import io.getstream.video.android.events.ParticipantJoinedEvent
import io.getstream.video.android.events.ParticipantLeftEvent
import io.getstream.video.android.utils.updateValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import stream.video.sfu.models.CallState

public class Call(
    private val context: Context,
    private val getCurrentUserId: () -> String,
    private val eglBase: EglBase,
) {

    private val logger = StreamLog.getLogger("Call:Room")

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

    public val localParticipantId: String
        get() = getCurrentUserId()

    public val localParticipantIdPrefix: String
        get() = _localParticipant.value?.idPrefix ?: ""

    private val audioManager by lazy {
        context.getSystemService<AudioManager>()
    }

    private val remoteAudioTracks = mutableSetOf<AudioTrack>()

    public var onStreamAdded: (MediaStream) -> Unit = {}
    public var onStreamRemoved: (MediaStream) -> Unit = {}

    /**
     * Public API.
     */
    public fun initRenderer(
        videoRenderer: SurfaceViewRenderer,
        streamId: String,
        onRender: (View) -> Unit = {}
    ) {
        logger.d { "[initRenderer] #sfu; streamId: $streamId" }
        videoRenderer.init(
            eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    logger.v { "[initRenderer.onFirstFrameRendered] #sfu; streamId: $streamId" }
                    updateParticipantTrackSize(
                        streamId,
                        videoRenderer.measuredWidth,
                        videoRenderer.measuredHeight
                    )
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.v { "[initRenderer.onFrameResolutionChanged] #sfu; streamId: $streamId" }
                }
            }
        )
    }

    private fun updateParticipantTrackSize(
        streamId: String,
        measuredWidth: Int,
        measuredHeight: Int
    ) {
        val oldState = _callParticipants.value

        val newState = oldState.updateValue(
            predicate = { it.id in streamId },
            transformer = { it.copy(trackSize = measuredWidth to measuredHeight) }
        )

        _callParticipants.value = newState
    }

    internal fun addStream(mediaStream: MediaStream) {
        logger.i { "[addStream] #sfu; mediaStream: $mediaStream" }
        if (mediaStream.audioTracks.isNotEmpty()) {
            mediaStream.audioTracks.forEach { track ->
                logger.v { "[addStream] #sfu; audioTrack: ${track.stringify()}" }
                track.setEnabled(true)
            }

            remoteAudioTracks.addAll(mediaStream.audioTracks)
            logger.d { "[addStream] #sfu; remoteAudioTracks: $remoteAudioTracks" }
        }

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.idPrefix in mediaStream.id },
            transformer = {
                val track = replaceTrackIfNeeded(mediaStream, it.track?.streamId)

                if (track != null) {
                    logger.d { "[addStream] updating users with track $track" }
                    track.video.setEnabled(true)
                    it.copy(track = track)
                } else {
                    it
                }
            }
        )

        logger.d { "[addStream] updated list $updatedList" }
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

    internal fun removeStream(mediaStream: MediaStream) {
        logger.d { "[removeStream] #sfu; mediaStream: $mediaStream" }
        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id in mediaStream.id },
            transformer = {
                it.copy(track = null, trackSize = 0 to 0)
            }
        )

        _callParticipants.value = updatedList
        onStreamRemoved(mediaStream)
    }

    internal fun loadParticipants(callState: CallState, callSettings: CallSettings) {
        val allParticipants =
            callState.participants.map {
                val user = it.toCallParticipant(getCurrentUserId())

                if (it.user?.id == getCurrentUserId()) {
                    user.hasAudio = callSettings.microphoneOn
                    user.hasVideo = callSettings.cameraOn
                }

                user
            }

        this._callParticipants.value = allParticipants
        logger.d { "[loadParticipants] #sfu; allParticipants: ${_callParticipants.value}" }

        this._localParticipant.value = allParticipants.firstOrNull { it.isLocal }
        logger.v { "[loadParticipants] #sfu; localParticipants: ${_localParticipant.value}" }
    }

    internal fun updateMuteState(event: MuteStateChangeEvent) {
        logger.d { "[updateMuteState] #sfu; event: $event" }
        val currentParticipants = _callParticipants.value

        val updatedList = currentParticipants.updateValue(
            predicate = { it.id == event.userId },
            transformer = {
                it.copy(
                    hasAudio = !event.audioMuted,
                    hasVideo = !event.videoMuted
                )
            }
        )

        _callParticipants.value = updatedList
    }

    internal fun addParticipant(event: ParticipantJoinedEvent) {
        logger.d { "[addParticipant] #sfu; event: $event" }
        _callParticipants.value =
            _callParticipants.value + event.participant.toCallParticipant(getCurrentUserId())
    }

    internal fun removeParticipant(event: ParticipantLeftEvent) {
        logger.d { "[removeParticipant] #sfu; event: $event" }
        val userId = event.participant.user?.id ?: return

        _callParticipants.value = _callParticipants.value.filter { it.id != userId }
    }

    private fun selectPrimaryParticipant(
        participantsList: List<CallParticipantState>,
        speakers: List<CallParticipantState>,
    ): CallParticipantState? {
        var speaker = lastPrimarySpeaker
        val localParticipant = participantsList.firstOrNull { it.isLocal }

        if (speaker?.isLocal == true) {
            val remoteSpeaker = // Try not to display local participant as speaker.
                participantsList.filter { !it.isLocal }.maxByOrNull { it.audioLevel }

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        // If previous primary speaker leaves
        if (!participantsList.contains(speaker)) {
            // Default to another person in room, or local participant.
            speaker = participantsList.filter { !it.isLocal }.maxByOrNull { it.audioLevel }
                ?: localParticipant
        }

        if (speakers.isNotEmpty() && !speakers.contains(speaker)) {
            val remoteSpeaker = // Try not to display local participant as speaker.
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
            track = videoTrack
        )

        val updated = allParticipants.updateValue(
            predicate = { it.id == getCurrentUserId() },
            transformer = {
                it.copy(track = videoTrack)
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
        _callParticipants.value.forEach {
            val track = it.track
            it.track = null
            track?.video?.dispose()
        }
        _callParticipants.value = emptyList()
    }

    public fun getLocalParticipant(): CallParticipantState? {
        return _callParticipants.value.firstOrNull { it.isLocal }
    }

    public fun setCameraEnabled(isEnabled: Boolean) {
        logger.d { "[setCameraEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _localParticipant.value
        val updatedLocal = localParticipant?.copy(hasVideo = isEnabled)
        _localParticipant.value = updatedLocal

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id == getCurrentUserId() },
            transformer = { it.copy(hasVideo = isEnabled) }
        )

        _callParticipants.value = updatedList
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        logger.d { "[setMicrophoneEnabled] #sfu; isEnabled: $isEnabled" }
        val localParticipant = _localParticipant.value
        val updatedLocal = localParticipant?.copy(hasAudio = isEnabled)
        _localParticipant.value = updatedLocal

        val updatedList = _callParticipants.value.updateValue(
            predicate = { it.id == getCurrentUserId() },
            transformer = { it.copy(hasAudio = isEnabled) }
        )

        _callParticipants.value = updatedList
    }

    internal fun updateAudioLevel(event: AudioLevelChangedEvent) {
        logger.d { "[updateAudioLevel] #sfu; isEnabled: $event" }

        val current = _callParticipants.value.toMutableList()

        event.levels.forEach { (userId, level) ->
            val index = current.indexOfFirst { it.id == userId }

            if (index != -1) {
                val updatedUser = current[index].copy(audioLevel = level)
                current.removeAt(index)
                current.add(index, updatedUser)
            }
        }

        _callParticipants.value = current.sortedByDescending { it.audioLevel }
    }
}
