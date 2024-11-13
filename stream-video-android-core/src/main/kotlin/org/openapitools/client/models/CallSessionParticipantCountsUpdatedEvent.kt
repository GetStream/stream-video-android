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





import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * This event is sent when the participant counts in a call session are updated
 *
 * @param anonymousParticipantCount
 * @param callCid
 * @param createdAt
 * @param participantsCountByRole
 * @param sessionId Call session ID
 * @param type The type of event: \"call.session_participant_count_updated\" in this case
 */


data class CallSessionParticipantCountsUpdatedEvent (

    @Json(name = "anonymous_participant_count")
    val anonymousParticipantCount: kotlin.Int,

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "participants_count_by_role")
    val participantsCountByRole: kotlin.collections.Map<kotlin.String, kotlin.Int>,

    /* Call session ID */
    @Json(name = "session_id")
    val sessionId: kotlin.String,

    /* The type of event: \"call.session_participant_count_updated\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "call.session_participant_count_updated"

) : VideoEvent(), WSCallEvent {

    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return type
    }
}
