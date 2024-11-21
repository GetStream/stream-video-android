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
import androidx.annotation.RawRes
import io.getstream.log.StreamLog
import io.getstream.video.android.core.R
import io.getstream.video.android.core.utils.safeCallWithDefault

// Interface & API
/**
 * Interface representing a ringing configuration.
 *
 * @see defaultResourcesRingingConfig
 * @see deviceRingtoneRingingConfig
 * @see emptyRingingConfig
 * @see resRingingConfig
 * @see uriRingingConfig
 */
public interface RingingConfig {
    val incomingCallSoundUri: Uri?
    val outgoingCallSoundUri: Uri?
}

/**
 * Contains all the sounds that the SDK uses.
 */
@Deprecated(
    message = "Sounds will be deprecated in the future and replaced with RingingConfig. It is recommended to use one of the factory methods along with toSounds() to create the Sounds object.",
    replaceWith = ReplaceWith("SoundConfig"),
    level = DeprecationLevel.WARNING,
)
public data class Sounds(val ringingConfig: RingingConfig) {
    @Deprecated(
        message = "Deprecated. This Constructor will now return a sound configuration with no sounds. Use constructor with SoundConfig parameter instead.",
        replaceWith = ReplaceWith("defaultResourcesRingingConfig(context).toSounds()"),
        level = DeprecationLevel.HIDDEN,
    )
    constructor(
        @RawRes incomingCallSound: Int = R.raw.call_incoming_sound,
        @RawRes outgoingCallSound: Int = R.raw.call_outgoing_sound,
    ) : this(emptyRingingConfig())
}

// Factories
/**
 * Returns a ringing config that uses the SDK default sounds for incoming and outgoing calls.
 *
 * @param context Context used for retrieving the sounds.
 */
public fun defaultResourcesRingingConfig(context: Context): RingingConfig = object : RingingConfig {
    override val incomingCallSoundUri: Uri? = R.raw.call_incoming_sound.toUriOrNUll(context)
    override val outgoingCallSoundUri: Uri? = R.raw.call_outgoing_sound.toUriOrNUll(context)
}

/**
 * Returns a ringing config that uses the device ringtone for incoming calls and the SDK default ringing tone for outgoing calls.
 *
 * @param context Context used for retrieving the sounds.
 */
public fun deviceRingtoneRingingConfig(context: Context): RingingConfig = object : RingingConfig {
    private val streamResSoundConfig = defaultResourcesRingingConfig(context)
    override val incomingCallSoundUri: Uri?
        get() = safeCallWithDefault(default = null) {
            RingtoneManager.getActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
            )
        } ?: streamResSoundConfig.incomingCallSoundUri
    override val outgoingCallSoundUri: Uri? = streamResSoundConfig.outgoingCallSoundUri
}

/**
 * Returns a ringing config that uses custom resources for incoming and outgoing call sounds.
 *
 * @param context Context used for retrieving the sounds.
 * @param incomingCallSoundResId The resource ID for the incoming call sound.
 * @param outgoingCallSoundResId The resource ID for the outgoing call sound.
 */
public fun resRingingConfig(
    context: Context,
    @RawRes incomingCallSoundResId: Int,
    @RawRes outgoingCallSoundResId: Int,
) = object : RingingConfig {
    override val incomingCallSoundUri: Uri? = incomingCallSoundResId.toUriOrNUll(context)
    override val outgoingCallSoundUri: Uri? = outgoingCallSoundResId.toUriOrNUll(context)
}

/**
 * Returns a ringing config that uses custom URIs for incoming and outgoing call sounds.
 *
 * @param incomingCallSoundUri The URI for the incoming call sound.
 * @param outgoingCallSoundUri The URI for the outgoing call sound.
 */
public fun uriRingingConfig(
    incomingCallSoundUri: Uri,
    outgoingCallSoundUri: Uri,
) = object : RingingConfig {
    override val incomingCallSoundUri: Uri = incomingCallSoundUri
    override val outgoingCallSoundUri: Uri = outgoingCallSoundUri
}

/**
 * Returns a ringing config that mutes (disables) incoming and outgoing call sounds.
 */
public fun emptyRingingConfig(): RingingConfig = object : RingingConfig {
    override val incomingCallSoundUri: Uri? = null
    override val outgoingCallSoundUri: Uri? = null
}

/**
 * Converts a ringing config to a [Sounds] object.
 */
public fun RingingConfig.toSounds() = Sounds(this)

// Internal utilities
private fun Int?.toUriOrNUll(context: Context): Uri? =
    safeCallWithDefault(default = null) {
        if (this != null) {
            Uri.parse("android.resource://${context.packageName}/$this")
        } else {
            StreamLog.w("RingingConfig") { "Resource ID is null. Returning null URI." }
            null
        }
    }
