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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.openapitools.client.models

import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.MemberResponse
import org.openapitools.client.models.UserResponse




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * This event is sent to call members who did not accept/reject/join the call to notify they missed the call
 *
 * @param call
 * @param callCid
 * @param createdAt
 * @param members List of members who missed the call
 * @param sessionId Call session ID
 * @param type The type of event: \"call.notification\" in this case
 * @param user
 */


data class CallMissedEvent (

    @Json(name = "call")
    val call: CallResponse,

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    /* List of members who missed the call */
    @Json(name = "members")
    val members: kotlin.collections.List<MemberResponse>,

    /* Call session ID */
    @Json(name = "session_id")
    val sessionId: kotlin.String,

    /* The type of event: \"call.notification\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "call.missed",

    @Json(name = "user")
    val user: UserResponse

) : VideoEvent(), WSCallEvent {

    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return type
    }
}
