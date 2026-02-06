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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * [All possibility of string to use]
 */
/**
 * OwnCapability Enum
 */
sealed class OwnCapability(val value: kotlin.String) {
    override fun toString(): String = value

    companion object {
        fun fromString(s: kotlin.String): OwnCapability = when (s) {
            "block-users" -> BlockUsers
            "change-max-duration" -> ChangeMaxDuration
            "create-call" -> CreateCall
            "create-reaction" -> CreateReaction
            "enable-noise-cancellation" -> EnableNoiseCancellation
            "end-call" -> EndCall
            "join-backstage" -> JoinBackstage
            "join-call" -> JoinCall
            "join-ended-call" -> JoinEndedCall
            "kick-user" -> KickUser
            "mute-users" -> MuteUsers
            "pin-for-everyone" -> PinForEveryone
            "read-call" -> ReadCall
            "remove-call-member" -> RemoveCallMember
            "screenshare" -> Screenshare
            "send-audio" -> SendAudio
            "send-closed-captions-call" -> SendClosedCaptionsCall
            "send-video" -> SendVideo
            "start-broadcast-call" -> StartBroadcastCall
            "start-closed-captions-call" -> StartClosedCaptionsCall
            "start-frame-record-call" -> StartFrameRecordCall
            "start-individual-record-call" -> StartIndividualRecordCall
            "start-raw-record-call" -> StartRawRecordCall
            "start-record-call" -> StartRecordCall
            "start-transcription-call" -> StartTranscriptionCall
            "stop-broadcast-call" -> StopBroadcastCall
            "stop-closed-captions-call" -> StopClosedCaptionsCall
            "stop-frame-record-call" -> StopFrameRecordCall
            "stop-individual-record-call" -> StopIndividualRecordCall
            "stop-raw-record-call" -> StopRawRecordCall
            "stop-record-call" -> StopRecordCall
            "stop-transcription-call" -> StopTranscriptionCall
            "update-call" -> UpdateCall
            "update-call-member" -> UpdateCallMember
            "update-call-permissions" -> UpdateCallPermissions
            "update-call-settings" -> UpdateCallSettings
            else -> Unknown(s)
        }
    }
    object BlockUsers : OwnCapability("block-users")
    object ChangeMaxDuration : OwnCapability("change-max-duration")
    object CreateCall : OwnCapability("create-call")
    object CreateReaction : OwnCapability("create-reaction")
    object EnableNoiseCancellation : OwnCapability("enable-noise-cancellation")
    object EndCall : OwnCapability("end-call")
    object JoinBackstage : OwnCapability("join-backstage")
    object JoinCall : OwnCapability("join-call")
    object JoinEndedCall : OwnCapability("join-ended-call")
    object KickUser : OwnCapability("kick-user")
    object MuteUsers : OwnCapability("mute-users")
    object PinForEveryone : OwnCapability("pin-for-everyone")
    object ReadCall : OwnCapability("read-call")
    object RemoveCallMember : OwnCapability("remove-call-member")
    object Screenshare : OwnCapability("screenshare")
    object SendAudio : OwnCapability("send-audio")
    object SendClosedCaptionsCall : OwnCapability("send-closed-captions-call")
    object SendVideo : OwnCapability("send-video")
    object StartBroadcastCall : OwnCapability("start-broadcast-call")
    object StartClosedCaptionsCall : OwnCapability("start-closed-captions-call")
    object StartFrameRecordCall : OwnCapability("start-frame-record-call")
    object StartIndividualRecordCall : OwnCapability("start-individual-record-call")
    object StartRawRecordCall : OwnCapability("start-raw-record-call")
    object StartRecordCall : OwnCapability("start-record-call")
    object StartTranscriptionCall : OwnCapability("start-transcription-call")
    object StopBroadcastCall : OwnCapability("stop-broadcast-call")
    object StopClosedCaptionsCall : OwnCapability("stop-closed-captions-call")
    object StopFrameRecordCall : OwnCapability("stop-frame-record-call")
    object StopIndividualRecordCall : OwnCapability("stop-individual-record-call")
    object StopRawRecordCall : OwnCapability("stop-raw-record-call")
    object StopRecordCall : OwnCapability("stop-record-call")
    object StopTranscriptionCall : OwnCapability("stop-transcription-call")
    object UpdateCall : OwnCapability("update-call")
    object UpdateCallMember : OwnCapability("update-call-member")
    object UpdateCallPermissions : OwnCapability("update-call-permissions")
    object UpdateCallSettings : OwnCapability("update-call-settings")
    data class Unknown(val unknownValue: kotlin.String) : OwnCapability(unknownValue)


    class OwnCapabilityAdapter : JsonAdapter<OwnCapability>() {
        @FromJson
        override fun fromJson(reader: JsonReader): OwnCapability? {
            val s = reader.nextString() ?: return null
            return OwnCapability.fromString(s)
        }

        @ToJson
        override fun toJson(writer: JsonWriter, value: OwnCapability?) {
            writer.value(value?.value)
        }
    }

}
