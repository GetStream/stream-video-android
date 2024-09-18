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
import android.media.RingtoneManager
import android.net.Uri
import io.getstream.video.android.core.R
import io.getstream.video.android.core.utils.safeCall

/**
 * Interface representing a sound configuration.
 *
 * @see createDeviceRingtoneSoundConfig
 * @see createStreamResourcesSoundConfig
 * @see createEmptySoundConfig
 * @see createCustomSoundConfig
 */
interface SoundConfig {

    val incomingCallSoundUri: Uri?
    val outgoingCallSoundUri: Uri?

    companion object {

        /**
         * Returns a sound config that uses the device ringtone for incoming calls and the SDK default ringing tone for outgoing calls.
         *
         * @param context Context used for retrieving the sounds.
         */
        fun createDeviceRingtoneSoundConfig(context: Context): SoundConfig = object : SoundConfig {

            private val streamResSoundConfig = createStreamResourcesSoundConfig(context)

            override val incomingCallSoundUri: Uri?
                get() = safeCall(default = null) {
                    RingtoneManager.getActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_RINGTONE,
                    )
                } ?: streamResSoundConfig.incomingCallSoundUri

            override val outgoingCallSoundUri: Uri? = streamResSoundConfig.outgoingCallSoundUri
        }

        /**
         * Returns a sound config that uses the SDK default sounds for incoming and outgoing calls.
         *
         * @param context Context used for retrieving the sounds.
         */
        fun createStreamResourcesSoundConfig(context: Context): SoundConfig = object : SoundConfig {

            override val incomingCallSoundUri: Uri? =
                getSoundUriFromRes(context, R.raw.call_incoming_sound)
            override val outgoingCallSoundUri: Uri? =
                getSoundUriFromRes(context, R.raw.call_outgoing_sound)
        }

        /**
         * Utility method that returns a sound URI from a resource ID.
         *
         * @return The sound URI or null if the resource ID is null or an exception occurred.
         */
        fun getSoundUriFromRes(context: Context, soundResId: Int?): Uri? = soundResId?.let {
            safeCall(default = null) {
                Uri.parse("android.resource://${context.packageName}/$soundResId")
            }
        }

        /**
         * Returns a sound config that mutes (disables) all sounds.
         */
        fun createEmptySoundConfig(): SoundConfig = object : SoundConfig {

            override val incomingCallSoundUri: Uri? = null
            override val outgoingCallSoundUri: Uri? = null
        }

        /**
         * Returns a sound config that uses custom sounds for incoming and outgoing calls.
         *
         * @param incomingCallSound The incoming call sound. Can be a resource ID or a URI.
         * @param outgoingCallSound The outgoing call sound. Can be a resource ID or a URI.
         * @param context Context used for retrieving the sounds. Mandatory when one of the sound parameters is a resource ID.
         *
         * @return A sound config with the provided sounds.
         *
         * @throws IllegalArgumentException If one of the sound parameters is a resource ID and the context is not provided.
         */
        fun createCustomSoundConfig(
            incomingCallSound: Any?,
            outgoingCallSound: Any?,
            context: Context? = null,
        ) = object : SoundConfig {

            override val incomingCallSoundUri: Uri? = when (incomingCallSound) {
                is Uri -> incomingCallSound
                is Int -> {
                    requireNotNull(
                        context,
                    ) { "Context is required when incomingCallSound is a resource ID." }
                    getSoundUriFromRes(context, incomingCallSound)
                }
                else -> null
            }

            override val outgoingCallSoundUri: Uri? = when (outgoingCallSound) {
                is Uri -> outgoingCallSound
                is Int -> {
                    requireNotNull(
                        context,
                    ) { "Context is required when outgoingCallSound is a resource ID." }
                    getSoundUriFromRes(context, outgoingCallSound)
                }
                else -> null
            }
        }
    }
}
