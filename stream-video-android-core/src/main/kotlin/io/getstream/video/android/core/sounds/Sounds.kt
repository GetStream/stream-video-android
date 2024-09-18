/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.sounds

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import io.getstream.video.android.core.R
import org.jetbrains.annotations.ApiStatus

/**
 * Contains all the sounds that the SDK uses.
 *
 * @param incomingCallSound Resource used as a ringtone for incoming calls.
 * @param outgoingCallSound Resource used as a ringing tone for outgoing calls.
 */
data class Sounds
@Deprecated(
    message = "Deprecated. Use constructor with SoundConfig parameter instead.",
    replaceWith = ReplaceWith("Sounds(soundConfig: SoundConfig)"),
    level = DeprecationLevel.WARNING,
)
@ApiStatus.ScheduledForRemoval(inVersion = "1.0.18")
constructor(
    @RawRes val incomingCallSound: Int? = R.raw.call_incoming_sound,
    @RawRes val outgoingCallSound: Int? = R.raw.call_outgoing_sound,
) {

    private var soundConfig: SoundConfig? = null
        private set

    /**
     * Configure sounds by passing a [SoundConfig].
     *
     * @see SoundConfig.createDeviceRingtoneSoundConfig
     * @see SoundConfig.createStreamResourcesSoundConfig
     * @see SoundConfig.createEmptySoundConfig
     * @see SoundConfig.createCustomSoundConfig
     * @see SoundConfig
     */
    constructor(soundConfig: SoundConfig) : this() {
        this.soundConfig = soundConfig
    }

    internal fun getIncomingCallSoundUri(context: Context): Uri? = soundConfig.let { soundConfig ->
        if (soundConfig != null) {
            soundConfig.incomingCallSoundUri
        } else {
            incomingCallSound?.let {
                Uri.parse("android.resource://${context.packageName}/$it")
            }
        }
    }

    internal fun getOutgoingCallSoundUri(context: Context): Uri? = soundConfig.let { soundConfig ->
        if (soundConfig != null) {
            soundConfig.outgoingCallSoundUri
        } else {
            outgoingCallSound?.let {
                Uri.parse("android.resource://${context.packageName}/$it")
            }
        }
    }
}
