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
import io.getstream.video.android.core.R

/**
 * Returns a sound config that uses the device ringtone for incoming calls and the SDK default ringing tone for outgoing calls.
 *
 * @param context Context used for retrieving the sounds.
 */
fun deviceRingtoneSoundConfig(context: Context): SoundConfig = DeviceRingtoneSoundConfig(context)

/**
 * Sound config that uses the device ringtone for incoming calls and the SDK default ringing tone for outgoing calls.
 */
open class DeviceRingtoneSoundConfig(val context: Context) : StreamResSoundConfig(context) {
    override val incomingCallSoundUri: Uri?
        get() = RingtoneManager.getActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE,
        ) ?: super.incomingCallSoundUri
}

/**
 * Returns a sound config that uses the SDK default sounds for incoming and outgoing calls.
 *
 * @param context Context used for retrieving the sounds.
 */
fun streamResourcesSoundConfig(
    context: Context,
): SoundConfig = StreamResSoundConfig(context = context)

/**
 * Sound config that uses the SDK default sound resources for incoming and outgoing calls.
 */
open class StreamResSoundConfig(context: Context) : ResSoundConfig(
    context = context,
    incomingCallSoundResId = R.raw.call_incoming_sound,
    outgoingCallSoundResId = R.raw.call_outgoing_sound,
)

/**
 * Returns a sound config that mutes (disables) all sounds.
 */
fun mutedSoundConfig(): SoundConfig = MutedSoundConfig()

/**
 * Sound config that mutes (disables) all sounds.
 */
open class MutedSoundConfig : SoundConfig {
    override val incomingCallSoundUri: Uri? = null
    override val outgoingCallSoundUri: Uri? = null
}

/**
 * A class that represents a sound config that uses raw resources to specify the sounds.
 */
open class ResSoundConfig(
    context: Context,
    @RawRes incomingCallSoundResId: Int?,
    @RawRes outgoingCallSoundResId: Int?,
) : SoundConfig {

    override val incomingCallSoundUri: Uri? = parseSoundUri(context, incomingCallSoundResId)
    override val outgoingCallSoundUri: Uri? = parseSoundUri(context, outgoingCallSoundResId)

    protected fun parseSoundUri(context: Context, soundResId: Int?): Uri? = soundResId?.let {
        Uri.parse("android.resource://${context.packageName}/$soundResId")
    }
}

/**
 * A class that represents a sound config that uses URIs to specify the sounds.
 */
data class UriSoundConfig(
    val incomingCallSoundUriValue: Uri?,
    val outgoingCallSoundUriValue: Uri?,
) : SoundConfig {

    override val incomingCallSoundUri: Uri? = incomingCallSoundUriValue
    override val outgoingCallSoundUri: Uri? = outgoingCallSoundUriValue
}

/**
 * Generic sound configuration.
 *
 * @see deviceRingtoneSoundConfig
 * @see streamResourcesSoundConfig
 * @see mutedSoundConfig
 * @see DeviceRingtoneSoundConfig
 * @see StreamResSoundConfig
 * @see ResSoundConfig
 * @see UriSoundConfig
 */
interface SoundConfig {
    val incomingCallSoundUri: Uri?
    val outgoingCallSoundUri: Uri?
}
