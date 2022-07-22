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

package io.getstream.video.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.livekit.android.ConnectOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

public class CallViewModel : ViewModel() {

    private var url: String = ""
    private var token: String = ""

    private val _roomState: MutableStateFlow<Room?> = MutableStateFlow(null)
    public val roomState: Flow<Room> = _roomState.filterNotNull()

    private var _localParticipantState: MutableStateFlow<LocalParticipant?> = MutableStateFlow(null)
    public val localParticipant: Flow<LocalParticipant> = _localParticipantState.filterNotNull()

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    public val activeSpeakers: Flow<List<Participant>> =
        roomState.flatMapLatest { it::activeSpeakers.flow }

    private val _isCameraEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isCameraEnabled: Flow<Boolean> = _isCameraEnabled

    private val _isMicrophoneEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isMicrophoneEnabled: Flow<Boolean> = _isMicrophoneEnabled

    public val participantList: Flow<List<Participant>> =
        roomState
            .flatMapLatest { it::remoteParticipants.flow }
            .combine(localParticipant) { remote, local -> remote to local }
            .map { (remoteParticipants, localParticipant) ->
                listOf<Participant>(localParticipant) + remoteParticipants.values
            }

    private val mutablePrimarySpeaker = MutableStateFlow<Participant?>(null)
    public val primarySpeaker: StateFlow<Participant?> = mutablePrimarySpeaker

    private val mutableFlipVideoButtonEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    public val flipButtonVideoEnabled: StateFlow<Boolean> = mutableFlipVideoButtonEnabled

    public fun init(
        room: Room,
        url: String,
        token: String
    ) {
        this.url = url
        this.token = token

        viewModelScope.launch {
            room.connect(
                url = url,
                token = token,
                options = ConnectOptions(autoSubscribe = true)
            )

            _roomState.value = room
            _localParticipantState.value = room.localParticipant
            _isVideoInitialized.value = true

            setupLocalParticipant(room.localParticipant)
            setupEvents(room)

            handlePrimarySpeaker(emptyList(), emptyList(), room)

            combine(
                participantList,
                activeSpeakers,
            ) { participants, speakers -> participants to speakers }
                .combine(roomState) { pair, room -> pair to room }
                .collect { (participantPair, room) ->
                    val (participantList, speakers) = participantPair

                    handlePrimarySpeaker(
                        participantList,
                        speakers,
                        room
                    )
                }
        }
    }

    private fun setupLocalParticipant(localParticipant: LocalParticipant) {
        viewModelScope.launch {
            localParticipant.setMicrophoneEnabled(true)
            _isMicrophoneEnabled.value = true

            localParticipant.setCameraEnabled(true)
            _isCameraEnabled.value = true
        }
    }

    private fun setupEvents(room: Room) {
        viewModelScope.launch {
            room.events.collect { event -> processEvent(event) }
        }
    }

    private fun processEvent(event: RoomEvent): Unit = when (event) {
        is RoomEvent.Reconnecting -> {}
        is RoomEvent.Reconnected -> {}
        is RoomEvent.Disconnected -> {}
        is RoomEvent.ParticipantConnected -> Unit
        is RoomEvent.ParticipantDisconnected -> Unit
        is RoomEvent.ActiveSpeakersChanged -> Unit
        is RoomEvent.RoomMetadataChanged -> Unit
        is RoomEvent.ParticipantMetadataChanged -> Unit
        is RoomEvent.TrackMuted -> Unit
        is RoomEvent.TrackUnmuted -> Unit
        is RoomEvent.TrackPublished -> Unit
        is RoomEvent.TrackUnpublished -> Unit
        is RoomEvent.TrackSubscribed -> Unit
        is RoomEvent.TrackSubscriptionFailed -> Unit
        is RoomEvent.TrackUnsubscribed -> Unit
        is RoomEvent.TrackStreamStateChanged -> Unit
        is RoomEvent.TrackSubscriptionPermissionChanged -> Unit
        is RoomEvent.DataReceived -> Unit
        is RoomEvent.ConnectionQualityChanged -> Unit
        is RoomEvent.FailedToConnect -> Unit
        is RoomEvent.ParticipantPermissionsChanged -> Unit
    }

    private fun handlePrimarySpeaker(
        participantsList: List<Participant>,
        speakers: List<Participant>,
        room: Room?
    ) {

        var speaker = mutablePrimarySpeaker.value

        // If speaker is local participant (due to defaults),
        // attempt to find another remote speaker to replace with.
        if (speaker is LocalParticipant) {
            val remoteSpeaker = participantsList
                .filterIsInstance<RemoteParticipant>() // Try not to display local participant as speaker.
                .firstOrNull()

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        // If previous primary speaker leaves
        if (!participantsList.contains(speaker)) {
            // Default to another person in room, or local participant.
            speaker = participantsList.filterIsInstance<RemoteParticipant>()
                .firstOrNull()
                ?: room?.localParticipant
        }

        if (speakers.isNotEmpty() && !speakers.contains(speaker)) {
            val remoteSpeaker = speakers
                .filterIsInstance<RemoteParticipant>() // Try not to display local participant as speaker.
                .firstOrNull()

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        mutablePrimarySpeaker.value = speaker
    }

    override fun onCleared() {
        super.onCleared()
        val room = _roomState.value ?: return

        room.disconnect()
    }

    public fun toggleCamera(enabled: Boolean) {
        val participant = _localParticipantState.value ?: return

        viewModelScope.launch {
            participant.setCameraEnabled(enabled)
            _isCameraEnabled.value = enabled
        }
    }

    public fun toggleMicrophone(enabled: Boolean) {
        val participant = _localParticipantState.value ?: return

        viewModelScope.launch {
            participant.setMicrophoneEnabled(enabled)
            _isMicrophoneEnabled.value = enabled
        }
    }

    public fun flipCamera() {
        val room = _roomState.value ?: return

        val videoTrack = room.localParticipant.getTrackPublication(Track.Source.CAMERA)
            ?.track as? LocalVideoTrack
            ?: return

        val newOptions = when (videoTrack.options.position) {
            CameraPosition.FRONT -> LocalVideoTrackOptions(position = CameraPosition.BACK)
            CameraPosition.BACK -> LocalVideoTrackOptions(position = CameraPosition.FRONT)
            else -> LocalVideoTrackOptions()
        }

        videoTrack.restartTrack(newOptions)
    }

    public fun reconnect() {
        val room = _roomState.value ?: return

        mutablePrimarySpeaker.value = null
        room.disconnect()
        viewModelScope.coroutineContext.cancelChildren()

        viewModelScope.launch {
            init(room, url, token)
        }
    }
}
