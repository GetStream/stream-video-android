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

import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.MemberResponse




import com.squareup.moshi.Json

/**
 * This event is sent when a call is created. Clients receiving this event should check if the ringing  field is set to true and if so, show the call screen
 *
 * @param call
 * @param callCid
 * @param createdAt
 * @param members the members added to this call
 * @param ringing true when the call was created with ring enabled
 * @param type The type of event: \"call.created\" in this case
 */


data class CallCreatedEvent (

    @Json(name = "call")
    val call: CallResponse,

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    /* the members added to this call */
    @Json(name = "members")
    val members: kotlin.collections.List<MemberResponse>,

    /* true when the call was created with ring enabled */
    @Json(name = "ringing")
    val ringing: kotlin.Boolean,

    /* The type of event: \"call.created\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "call.created"

) : VideoEvent(), WSCallEvent{

    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return type
    }
}
