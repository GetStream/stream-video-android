/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

/**
 * Configuration describing the sounds played for incoming and outgoing calls
 * in the Stream Video SDK.
 *
 * The SDK allows customizing ringing behavior using either Android resources
 * or external audio URIs.
 *
 * Example using raw resources:
 * ```kotlin
 * StreamVideoBuilder(
 *     ringingConfig = resRingingConfig(context, R.raw.my_incoming_sound, R.raw.my_outgoing_sound),
 *     mutedRingingConfig = defaultMutedRingingConfig(),
 * )
 * ```
 *
 * Example using URIs:
 * ```kotlin
 * StreamVideoBuilder(
 *     ringingConfig = uriRingingConfig(incomingUri, outgoingUri),
 *     mutedRingingConfig = defaultMutedRingingConfig(),
 * )
 * ```
 *
 * If a sound URI is `null`, no sound will be played.
 *
 * Instances are typically created using one of the provided factory methods:
 * [defaultResourcesRingingConfig], [deviceRingtoneRingingConfig],
 * [resRingingConfig], [uriRingingConfig], or [emptyRingingConfig].
 *
 *
 * Creates a [RingingConfig] using externally provided audio URIs.
 *
 * @param incomingCallSoundUri URI of the sound played for incoming calls.
 * @param outgoingCallSoundUri URI of the sound played while the caller waits
 * for the callee to answer.
 *
 */
public interface RingingConfig {
    val incomingCallSoundUri: Uri?
    val outgoingCallSoundUri: Uri?
}

/**
 * Configuration that controls whether ringing sounds should play when
 * the device is in silent or muted mode.
 *
 * By default, ringing sounds are suppressed when the device is muted.
 * This configuration allows overriding that behavior for incoming
 * and outgoing call sounds.
 *
 * This configuration does not apply to incoming-call ringing owned by a notification channel.
 * Android does not provide a supported way for a notification ringtone to bypass silent mode.
 * It continues to apply when incoming ringing is played directly by the SDK and to outgoing
 * call sounds.
 *
 * @param playIncomingSoundIfMuted Whether the incoming call sound should play even if the device is muted.
 * @param playOutgoingSoundIfMuted Whether the outgoing ringing sound should play even if the device is muted.
 */
public interface MutedRingingConfig {
    val playIncomingSoundIfMuted: Boolean
    val playOutgoingSoundIfMuted: Boolean
}

/**
 * Returns a [RingingConfig] that uses the SDK's default bundled sounds
 * for both incoming and outgoing calls.
 *
 * @param context Used to resolve the internal audio resources.
 */
public fun defaultResourcesRingingConfig(
    context: Context,
): RingingConfig = object : RingingConfig {
    override val incomingCallSoundUri: Uri? = R.raw.call_incoming_sound.toUriOrNull(context)
    override val outgoingCallSoundUri: Uri? = R.raw.call_outgoing_sound.toUriOrNull(context)
}

/**
 * Creates a default [MutedRingingConfig].
 *
 * @param playIncomingSoundIfMuted Whether the incoming call sound should
 * still play when the device is muted.
 * @param playOutgoingSoundIfMuted Whether the outgoing ringing sound should
 * still play when the device is muted.
 */
public fun defaultMutedRingingConfig(
    playIncomingSoundIfMuted: Boolean = false,
    playOutgoingSoundIfMuted: Boolean = false,
): MutedRingingConfig = object : MutedRingingConfig {
    override val playIncomingSoundIfMuted: Boolean = playIncomingSoundIfMuted
    override val playOutgoingSoundIfMuted: Boolean = playOutgoingSoundIfMuted
}

/**
 * Returns a [RingingConfig] that uses the device ringtone for incoming calls and the SDK default ringing tone for outgoing calls.
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
 * Returns a [RingingConfig] that uses custom resources for incoming and outgoing call sounds.
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
 * Returns a [RingingConfig] that uses custom URIs for incoming and outgoing call sounds.
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
 * Returns a [RingingConfig] that disables all ringing sounds.
 *
 * Both incoming and outgoing call sounds will be muted.
 */
public fun emptyRingingConfig(): RingingConfig = object : RingingConfig {
    override val incomingCallSoundUri: Uri? = null
    override val outgoingCallSoundUri: Uri? = null
}

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
