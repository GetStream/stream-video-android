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
 * Values: blockMinusUsers,createMinusCall,createMinusReaction,endMinusCall,joinMinusBackstage,joinMinusCall,joinMinusEndedMinusCall,muteMinusUsers,readMinusCall,screenshare,sendMinusAudio,sendMinusVideo,startMinusBroadcastMinusCall,startMinusRecordMinusCall,stopMinusBroadcastMinusCall,stopMinusRecordMinusCall,updateMinusCall,updateMinusCallMinusPermissions,updateMinusCallMinusSettings
 */

enum class OwnCapability(val value: kotlin.String) {

    @Json(name = "block-users")
    blockMinusUsers("block-users"),

    @Json(name = "create-call")
    createMinusCall("create-call"),

    @Json(name = "create-reaction")
    createMinusReaction("create-reaction"),

    @Json(name = "end-call")
    endMinusCall("end-call"),

    @Json(name = "join-backstage")
    joinMinusBackstage("join-backstage"),

    @Json(name = "join-call")
    joinMinusCall("join-call"),

    @Json(name = "join-ended-call")
    joinMinusEndedMinusCall("join-ended-call"),

    @Json(name = "mute-users")
    muteMinusUsers("mute-users"),

    @Json(name = "read-call")
    readMinusCall("read-call"),

    @Json(name = "screenshare")
    screenshare("screenshare"),

    @Json(name = "send-audio")
    sendMinusAudio("send-audio"),

    @Json(name = "send-video")
    sendMinusVideo("send-video"),

    @Json(name = "start-broadcast-call")
    startMinusBroadcastMinusCall("start-broadcast-call"),

    @Json(name = "start-record-call")
    startMinusRecordMinusCall("start-record-call"),

    @Json(name = "stop-broadcast-call")
    stopMinusBroadcastMinusCall("stop-broadcast-call"),

    @Json(name = "stop-record-call")
    stopMinusRecordMinusCall("stop-record-call"),

    @Json(name = "update-call")
    updateMinusCall("update-call"),

    @Json(name = "update-call-permissions")
    updateMinusCallMinusPermissions("update-call-permissions"),

    @Json(name = "update-call-settings")
    updateMinusCallMinusSettings("update-call-settings"),

    @Json(name = "start-transcription-call")
    startTranscriptionCall("start-transcription-call"),

    @Json(name = "stop-transcription-call")
    stopTranscriptionCall("stop-transcription-call");

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
         * Returns a valid [OwnCapability] for [data], null otherwise.
         */
        fun decode(data: kotlin.Any?): OwnCapability? = data?.let {
            val normalizedData = "$it".lowercase()
            values().firstOrNull { value ->
                it == value || normalizedData == "$value".lowercase()
            }
        }
    }
}
