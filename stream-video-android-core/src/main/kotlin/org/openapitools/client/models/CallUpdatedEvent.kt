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
 * This event is sent when a call is updated, clients should use this update the local state of the call.  This event also contains the capabilities by role for the call, clients should update the own_capability for the current.
 *
 * @param call
 * @param callCid
 * @param capabilitiesByRole The capabilities by role for this call
 * @param createdAt
 * @param type The type of event: \"call.ended\" in this case
 */

data class CallUpdatedEvent(

    @Json(name = "call")
    val call: CallResponse,

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    /* The capabilities by role for this call */
    @Json(name = "capabilities_by_role")
    val capabilitiesByRole: kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.String>>,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    /* The type of event: \"call.ended\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "call.updated"

) : VideoEvent(), WSCallEvent {

    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return type
    }
}
