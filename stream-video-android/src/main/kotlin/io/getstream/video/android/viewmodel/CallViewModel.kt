package io.getstream.video.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

public class CallViewModel : ViewModel() {

    private val _roomState: MutableStateFlow<Room?> = MutableStateFlow(null)
    public val roomState: Flow<Room> = _roomState.filterNotNull()

    private val _participantsState: MutableStateFlow<List<Participant>> =
        MutableStateFlow(emptyList())
    public val participants: Flow<List<Participant>> = _participantsState

    private var _localParticipantState: MutableStateFlow<LocalParticipant?> = MutableStateFlow(null)
    public val localParticipant: Flow<LocalParticipant> = _localParticipantState.filterNotNull()

    private var _isVideoInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val isVideoInitialized: StateFlow<Boolean> = _isVideoInitialized

    private var _activeSpeaker: MutableStateFlow<Participant?> = MutableStateFlow(null)
    public val activeSpeaker: Flow<Participant> = _activeSpeaker.filterNotNull()

    public fun init(room: Room) {
        this._roomState.value = room
        this._localParticipantState.value = room.localParticipant
        this._isVideoInitialized.value = true

        setupLocalParticipant(room.localParticipant)
        setupEvents(room)
    }

    private fun setupLocalParticipant(localParticipant: LocalParticipant) {
        viewModelScope.launch {
            localParticipant.setMicrophoneEnabled(true)
            localParticipant.setCameraEnabled(true)
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
        is RoomEvent.ParticipantConnected -> addParticipant(event.participant)
        is RoomEvent.ParticipantDisconnected -> removeParticipant(event.participant)
        is RoomEvent.ActiveSpeakersChanged -> _activeSpeaker.value = event.speakers.firstOrNull()
        is RoomEvent.RoomMetadataChanged -> Unit
        is RoomEvent.ParticipantMetadataChanged -> Unit
        is RoomEvent.TrackMuted -> Unit
        is RoomEvent.TrackUnmuted -> Unit
        is RoomEvent.TrackPublished -> Unit
        is RoomEvent.TrackUnpublished -> Unit
        is RoomEvent.TrackSubscribed -> addParticipant(event.participant)
        is RoomEvent.TrackSubscriptionFailed -> Unit
        is RoomEvent.TrackUnsubscribed -> removeParticipant(event.participant)
        is RoomEvent.TrackStreamStateChanged -> Unit
        is RoomEvent.TrackSubscriptionPermissionChanged -> Unit
        is RoomEvent.DataReceived -> Unit
        is RoomEvent.ConnectionQualityChanged -> Unit
        is RoomEvent.FailedToConnect -> Unit
        is RoomEvent.ParticipantPermissionsChanged -> Unit
    }

    private fun addParticipant(participant: Participant) {
        val currentParticipants = _participantsState.value.toMutableList() + participant

        this._participantsState.value = currentParticipants.distinctBy { it.sid }
    }

    private fun removeParticipant(participant: Participant) {
        val currentParticipants = _participantsState.value.toMutableList()
        currentParticipants.removeAll { it.sid == participant.sid }

        this._participantsState.value = currentParticipants
    }
}