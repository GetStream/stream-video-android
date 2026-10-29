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

package io.getstream.android.video.api.generated.models

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
public sealed class OwnCapability(public val value: String) {
    override fun toString(): String = value

    public companion object {
        public fun fromString(s: String): OwnCapability = when (s) {
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
    public object BlockUsers : OwnCapability("block-users")
    public object ChangeMaxDuration : OwnCapability("change-max-duration")
    public object CreateCall : OwnCapability("create-call")
    public object CreateReaction : OwnCapability("create-reaction")
    public object EnableNoiseCancellation : OwnCapability("enable-noise-cancellation")
    public object EndCall : OwnCapability("end-call")
    public object JoinBackstage : OwnCapability("join-backstage")
    public object JoinCall : OwnCapability("join-call")
    public object JoinEndedCall : OwnCapability("join-ended-call")
    public object KickUser : OwnCapability("kick-user")
    public object MuteUsers : OwnCapability("mute-users")
    public object PinForEveryone : OwnCapability("pin-for-everyone")
    public object ReadCall : OwnCapability("read-call")
    public object RemoveCallMember : OwnCapability("remove-call-member")
    public object Screenshare : OwnCapability("screenshare")
    public object SendAudio : OwnCapability("send-audio")
    public object SendClosedCaptionsCall : OwnCapability("send-closed-captions-call")
    public object SendVideo : OwnCapability("send-video")
    public object StartBroadcastCall : OwnCapability("start-broadcast-call")
    public object StartClosedCaptionsCall : OwnCapability("start-closed-captions-call")
    public object StartFrameRecordCall : OwnCapability("start-frame-record-call")
    public object StartIndividualRecordCall : OwnCapability("start-individual-record-call")
    public object StartRawRecordCall : OwnCapability("start-raw-record-call")
    public object StartRecordCall : OwnCapability("start-record-call")
    public object StartTranscriptionCall : OwnCapability("start-transcription-call")
    public object StopBroadcastCall : OwnCapability("stop-broadcast-call")
    public object StopClosedCaptionsCall : OwnCapability("stop-closed-captions-call")
    public object StopFrameRecordCall : OwnCapability("stop-frame-record-call")
    public object StopIndividualRecordCall : OwnCapability("stop-individual-record-call")
    public object StopRawRecordCall : OwnCapability("stop-raw-record-call")
    public object StopRecordCall : OwnCapability("stop-record-call")
    public object StopTranscriptionCall : OwnCapability("stop-transcription-call")
    public object UpdateCall : OwnCapability("update-call")
    public object UpdateCallMember : OwnCapability("update-call-member")
    public object UpdateCallPermissions : OwnCapability("update-call-permissions")
    public object UpdateCallSettings : OwnCapability("update-call-settings")
    public data class Unknown(val unknownValue: String) : OwnCapability(unknownValue)


    public class OwnCapabilityAdapter : JsonAdapter<OwnCapability>() {
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
