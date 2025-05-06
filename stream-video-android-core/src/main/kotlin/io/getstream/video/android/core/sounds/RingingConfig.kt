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
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.utils.safeCallWithDefault
import io.getstream.video.android.model.StreamCallId

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

public interface MutedRingingConfig {
    val playIncomingSoundIfMuted: Boolean
    val playOutgoingSoundIfMuted: Boolean
}

/**
 * Contains all the sounds that the SDK uses.
 */
@Deprecated(
    message = "Sounds will be deprecated in the future and replaced with RingingConfig. It is recommended to use one of the factory methods along with toSounds() to create the Sounds object.",
    replaceWith = ReplaceWith("RingingConfig"),
    level = DeprecationLevel.WARNING,
)
public data class Sounds(
    val ringingConfig: RingingConfig,
    val mutedRingingConfig: MutedRingingConfig? = null,
    val audioCallRingingConfig: RingingConfig? = null,
    val videoCallRingingConfig: RingingConfig? = null,
)

// Factories
/**
 * Creates a [Sounds] object using the provided [RingingConfig] and [MutedRingingConfig].
 *
 * @param ringingConfig The configuration for incoming and outgoing call sounds.
 * @param mutedRingingConfig The configuration for handling incoming and outgoing call sounds if device is muted. Can be null.
 * @param audioCallRingingConfig The configuration for incoming and outgoing [CallType.AudioCall] sounds.
 * @param videoCallRingingConfig The configuration for incoming and outgoing [CallType.Default] sounds.
 * @return A [Sounds] object containing the specified configurations.
 *
 *
 * Prepares a [Sounds] configuration that specifies custom incoming and outgoing
 * sounds for different call types — audio and video.
 *
 * You can override the default sounds by placing `.mp3` or `.wav` files in your
 * `res/raw/` directory and referencing them via their resource IDs.
 *
 * ### Sample usage:
 *
 * ```kotlin
 * val audioRingingConfig = object : RingingConfig {
 *     override val incomingCallSoundUri: Uri
 *         get() = R.raw.audio_call_ringtone.toUriOrNull(context)
 *
 *     override val outgoingCallSoundUri: Uri
 *         get() = R.raw.audio_call_ringtone.toUriOrNull(context)
 * }
 *
 * val videoRingingConfig = object : RingingConfig {
 *     override val incomingCallSoundUri: Uri
 *         get() = R.raw.video_call_ringtone.toUriOrNull(context)
 *
 *     override val outgoingCallSoundUri: Uri
 *         get() = R.raw.video_call_ringtone.toUriOrNull(context)
 * }
 *
 * val sounds = Sounds(
 *     audioCallRingingConfig = audioRingingConfig,
 *     videoCallRingingConfig = videoRingingConfig,
 *     ringingConfig = audioRingingConfig // fallback/default
 * )
 * ```
 *
 */
public fun ringingConfig(
    ringingConfig: RingingConfig,
    mutedRingingConfig: MutedRingingConfig?,
    audioCallRingingConfig: RingingConfig?,
    videoCallRingingConfig: RingingConfig?,
): Sounds {
    return Sounds(ringingConfig, mutedRingingConfig, audioCallRingingConfig, videoCallRingingConfig)
}

/**
 * Returns a ringing config that uses the SDK default sounds for incoming and outgoing calls.
 *
 * @param context Context used for retrieving the sounds.
 */
public fun defaultResourcesRingingConfig(
    context: Context,
): RingingConfig = object : RingingConfig {
    override val incomingCallSoundUri: Uri? = R.raw.call_incoming_sound.toUriOrNull(context)
    override val outgoingCallSoundUri: Uri? = R.raw.call_outgoing_sound.toUriOrNull(context)
}

public fun defaultMutedRingingConfig(
    playIncomingSoundIfMuted: Boolean = false,
    playOutgoingSoundIfMuted: Boolean = false,
): MutedRingingConfig = object : MutedRingingConfig {
    override val playIncomingSoundIfMuted: Boolean = playIncomingSoundIfMuted
    override val playOutgoingSoundIfMuted: Boolean = playOutgoingSoundIfMuted
}

/**
 * Returns a ringing config that uses the device ringtone for incoming calls and the SDK default ringing tone for outgoing calls.
 *
 * @param context Context used for retrieving the sounds.
 */
public fun deviceRingtoneRingingConfig(
    context: Context,
): RingingConfig = object : RingingConfig {
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
    override val incomingCallSoundUri: Uri? = incomingCallSoundResId.toUriOrNull(context)
    override val outgoingCallSoundUri: Uri? = outgoingCallSoundResId.toUriOrNull(context)
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
private fun Int?.toUriOrNull(context: Context): Uri? =
    safeCallWithDefault(default = null) {
        if (this != null) {
            Uri.parse("android.resource://${context.packageName}/$this")
        } else {
            StreamLog.w("RingingConfig") { "Resource ID is null. Returning null URI." }
            null
        }
    }

internal fun getIncomingCallSoundUri(streamVideo: StreamVideoClient, callId: StreamCallId): Uri? {
    return when (callId.type) {
        CallType.AudioCall.name ->
            streamVideo.sounds.audioCallRingingConfig?.incomingCallSoundUri
                ?: streamVideo.sounds.ringingConfig.incomingCallSoundUri
        CallType.Default.name ->
            streamVideo.sounds.videoCallRingingConfig?.incomingCallSoundUri
                ?: streamVideo.sounds.ringingConfig.incomingCallSoundUri
        else -> streamVideo.sounds.ringingConfig.incomingCallSoundUri
    }
}

internal fun getOutgoingCallSoundUri(streamVideo: StreamVideoClient, callId: StreamCallId): Uri? {
    return when (callId.type) {
        CallType.AudioCall.name ->
            streamVideo.sounds.audioCallRingingConfig?.outgoingCallSoundUri
                ?: streamVideo.sounds.ringingConfig.outgoingCallSoundUri
        CallType.Default.name ->
            streamVideo.sounds.videoCallRingingConfig?.outgoingCallSoundUri
                ?: streamVideo.sounds.ringingConfig.outgoingCallSoundUri
        else -> streamVideo.sounds.ringingConfig.outgoingCallSoundUri
    }
}
