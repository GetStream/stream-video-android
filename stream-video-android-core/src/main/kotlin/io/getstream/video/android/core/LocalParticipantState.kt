package io.getstream.video.android.core

import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.OwnCapability
import stream.video.sfu.models.TrackType

public class LocalParticipantState(
    override val call: Call,
    override val initialUser: User,
    override var sessionId: String = "",
    override var idPrefix: String = "",
    override val isLocal: Boolean = false,
    override var isOnline: Boolean = false,

    override var videoTrack: VideoTrack? = null,
    override var screenSharingTrack: VideoTrack? = null,
    override var publishedTracks: Set<TrackType> = emptySet(),
    override var videoTrackSize: Pair<Int, Int> = Pair(0, 0)
) : ParticipantState(call, initialUser, sessionId,idPrefix, isLocal, isOnline, videoTrack, screenSharingTrack, publishedTracks, videoTrackSize
) {

    // TODO: we need to use data classes for this or comparisons won't work in stateflow
    // perhaps drop the whole class, otherwise we need to keep a different copy of this in the local state

    fun copy2(
        call: Call=this.call,
        initialUser: User = this.initialUser,
        sessionId: String = this.sessionId,
        idPrefix: String = this.idPrefix,
        isLocal: Boolean = this.isLocal,
        isOnline: Boolean = this.isOnline,
        videoTrack: VideoTrack? = this.videoTrack,
        screenSharingTrack: VideoTrack? = this.screenSharingTrack,
        publishedTracks: Set<TrackType> = this.publishedTracks,
        videoTrackSize: Pair<Int, Int> = this.videoTrackSize
    ): LocalParticipantState {
        return LocalParticipantState(
            call = call,
            initialUser = initialUser,
            sessionId = sessionId,
            idPrefix = idPrefix,
            isLocal = isLocal,
            isOnline = isOnline,
            videoTrack = videoTrack,
            screenSharingTrack = screenSharingTrack,
            publishedTracks = publishedTracks,
            videoTrackSize = videoTrackSize
        )
    }

    // call.activeSession!!.setMicrophoneEnabled(false)


    // call.activeSession!!.setCameraEnabled(false)

    fun flipCamera() {
        // TODO front and back facing
        call.activeSession!!.flipCamera()
    }

    internal val _ownCapabilities: MutableStateFlow<List<OwnCapability>> = MutableStateFlow(
        emptyList()
    )
    val ownCapabilities: StateFlow<List<OwnCapability>> = _ownCapabilities
    fun hasPermission(permission: String): Boolean {
        return ownCapabilities.value.any { it.name == permission }
    }


    val localVideoTrack by lazy {
        call.activeSession?.localVideoTrack
    }
    val localAudioTrack by lazy {
        call.activeSession?.localAudioTrack
    }


}