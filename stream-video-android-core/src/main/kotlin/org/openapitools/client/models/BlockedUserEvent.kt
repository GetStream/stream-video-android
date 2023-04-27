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
 * This event is sent to call participants to notify when a user is blocked on a call, clients can use this event to show a notification.  If the user is the current user, the client should leave the call screen as well
 *
 * @param callCid * @param createdAt * @param type The type of event: \"call.blocked_user\" in this case
 * @param user * @param blockedByUser */

data class BlockedUserEvent(

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    /* The type of event: \"call.blocked_user\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "call.blocked_user",

    @Json(name = "user")
    val user: UserResponse,

    @Json(name = "blocked_by_user")
    val blockedByUser: UserResponse? = null

) : VideoEvent(), WSCallEvent {

    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return type
    }
}
