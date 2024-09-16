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
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R

/**
 * Contains all the sounds that the SDK uses.
 *
 * @param context Context to be used for retrieving the sounds.
 * @param incomingCallSoundResId Resource to be used as a ringtone for incoming calls. Set to [DEVICE_INCOMING_RINGTONE] to use the device ringtone.
 * @param outgoingCallSoundResId Resource to be used as a ringing tone for outgoing calls.
 */
class Sounds(
    private val context: Context,
    @RawRes private val incomingCallSoundResId: Int? = DEVICE_INCOMING_RINGTONE,
    @RawRes private val outgoingCallSoundResId: Int? = R.raw.call_outgoing_sound,
) {
    private val logger by taggedLogger("StreamVideo:Sounds")

    internal val incomingCallSoundUri: Uri?
        get() = if (incomingCallSoundResId == DEVICE_INCOMING_RINGTONE) {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE) ?: run {
                logger.w { "Device ringtone was null. Falling back to default incoming call sound." }
                parseSoundUri(R.raw.call_incoming_sound)
            }
        } else {
            parseSoundUri(incomingCallSoundResId)
        }

    internal val outgoingCallSoundUri: Uri?
        get() = if (outgoingCallSoundResId == DEVICE_INCOMING_RINGTONE) {
            logger.w {
                "Cannot assign DEVICE_INCOMING_RINGTONE to Sounds#outgoingCallSoundResId. Falling back to default outgoing call sound."
            }
            parseSoundUri(R.raw.call_outgoing_sound)
        } else {
            parseSoundUri(outgoingCallSoundResId)
        }

    private fun parseSoundUri(@RawRes soundResId: Int?) = soundResId?.let {
        Uri.parse("android.resource://${context.packageName}/$soundResId")
    }

    companion object {
        @RawRes const val DEVICE_INCOMING_RINGTONE = -1
    }
}
