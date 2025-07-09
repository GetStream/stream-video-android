package io.getstream.video.android.client.api.state.state

import stream.video.sfu.models.Participant

/**
 * The state of the call.
 */
public interface StreamCallState {

    public fun getParticipants(): List<Participant>
}