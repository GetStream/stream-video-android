package io.getstream.video.android.core

import io.getstream.video.android.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.OwnCapability

public class LocalParticipantState(override val call: Call2, user: User) : ParticipantState(
    call = call,
    initialUser = user
) {
    override fun muteAudio() {
        call.activeSession!!.setMicrophoneEnabled(false)
    }

    override fun muteVideo() {
        // TODO: raise a nice error if the session ins't there yet
        call.activeSession!!.setCameraEnabled(false)
    }

    fun flipCamera() {
        // TODO front and back facing
        call.activeSession!!.flipCamera()
    }

    internal val _ownCapabilities: MutableStateFlow<List<OwnCapability>> = MutableStateFlow(
        emptyList()
    )
    val ownCapabilities: StateFlow<List<OwnCapability>> = _ownCapabilities

    val localVideoTrack by lazy {
        call.activeSession?.localVideoTrack
    }
    val localAudioTrack by lazy {
        call.activeSession?.localAudioTrack
    }


}