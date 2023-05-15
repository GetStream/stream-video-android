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

import org.openapitools.client.models.UserResponse




import com.squareup.moshi.Json

/**
 * This event is sent when a participant joins a call session
 *
 * @param callCid
 * @param createdAt
 * @param sessionId Call session ID
 * @param type The type of event: \"call.session_participant_joined\" in this case
 * @param user
 */


data class CallSessionParticipantJoinedEvent (

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    /* Call session ID */
    @Json(name = "session_id")
    val sessionId: kotlin.String,

    /* The type of event: \"call.session_participant_joined\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "call.session_participant_joined",

    @Json(name = "user")
    val user: UserResponse

) : VideoEvent(), WSCallEvent{

    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return type
    }
}
