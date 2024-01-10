package io.getstream.video.android.core.sounds

import androidx.annotation.RawRes
import io.getstream.video.android.core.R

/**
 * Contains all the sounds that the SDK uses.
 *
 * @param incomingCallSound Resource used as a ringtone for incoming calls.
 * @param outgoingCallSound Resource used as a ringing tone for outgoing calls.
 */
data class Sounds(
    @RawRes val incomingCallSound: Int = R.raw.call_incoming_sound,
    @RawRes val outgoingCallSound: Int = R.raw.call_outgoing_sound,
)