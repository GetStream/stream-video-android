/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package org.openapitools.client.models

import com.squareup.moshi.Json

/**
 * All possibility of string to use
 *
 * Values: blockUsers,createCall,createReaction,endCall,joinBackstage,joinCall,joinEndedCall,muteUsers,readCall,removeCallMember,screenshare,sendAudio,sendVideo,startBroadcastCall,startRecordCall,startTranscriptionCall,stopBroadcastCall,stopRecordCall,stopTranscriptionCall,updateCall,updateCallMember,updateCallPermissions,updateCallSettings
 */

enum class OwnCapability(val value: kotlin.String) {

    @Json(name = "block-users")
    blockUsers("block-users"),

    @Json(name = "create-call")
    createCall("create-call"),

    @Json(name = "create-reaction")
    createReaction("create-reaction"),

    @Json(name = "end-call")
    endCall("end-call"),

    @Json(name = "join-backstage")
    joinBackstage("join-backstage"),

    @Json(name = "join-call")
    joinCall("join-call"),

    @Json(name = "join-ended-call")
    joinEndedCall("join-ended-call"),

    @Json(name = "mute-users")
    muteUsers("mute-users"),

    @Json(name = "read-call")
    readCall("read-call"),

    @Json(name = "remove-call-member")
    removeCallMember("remove-call-member"),

    @Json(name = "screenshare")
    screenshare("screenshare"),

    @Json(name = "send-audio")
    sendAudio("send-audio"),

    @Json(name = "send-video")
    sendVideo("send-video"),

    @Json(name = "start-broadcast-call")
    startBroadcastCall("start-broadcast-call"),

    @Json(name = "start-record-call")
    startRecordCall("start-record-call"),

    @Json(name = "start-transcription-call")
    startTranscriptionCall("start-transcription-call"),

    @Json(name = "stop-broadcast-call")
    stopBroadcastCall("stop-broadcast-call"),

    @Json(name = "stop-record-call")
    stopRecordCall("stop-record-call"),

    @Json(name = "stop-transcription-call")
    stopTranscriptionCall("stop-transcription-call"),

    @Json(name = "update-call")
    updateCall("update-call"),

    @Json(name = "update-call-member")
    updateCallMember("update-call-member"),

    @Json(name = "update-call-permissions")
    updateCallPermissions("update-call-permissions"),

    @Json(name = "update-call-settings")
    updateCallSettings("update-call-settings"),

    /**
     * This case is used when decoding an unknown value.
     */
    unknown("unknown");

    /**
     * Override toString() to avoid using the enum variable name as the value, and instead use
     * the actual value defined in the API spec file.
     *
     * This solves a problem when the variable name and its value are different, and ensures that
     * the client sends the correct enum values to the server always.
     */
    override fun toString(): String = value

    companion object {
        /**
         * Converts the provided [data] to a [String] on success, null otherwise.
         */
        fun encode(data: kotlin.Any?): kotlin.String? = if (data is OwnCapability) "$data" else null

        /**
         * Returns a valid [OwnCapability] for [data], unknown otherwise.
         */
        fun decode(data: kotlin.Any?): OwnCapability? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            } ?: unknown
        }
    }
}
