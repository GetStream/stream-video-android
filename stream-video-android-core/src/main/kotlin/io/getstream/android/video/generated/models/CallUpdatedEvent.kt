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
    "UnusedImport",
)

package io.getstream.android.video.generated.models

import com.squareup.moshi.Json
import kotlin.collections.List
import kotlin.collections.Map

/**
 * This event is sent when a call is updated, clients should use this update the local state of the call.
 */

data class CallUpdatedEvent(
    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "call")
    val call: io.getstream.android.video.generated.models.CallResponse,

    @Json(name = "capabilities_by_role")
    val capabilitiesByRole:
    kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.String>> = emptyMap(),

    @Json(name = "type")
    val type: kotlin.String = "call.updated",
) :
    io.getstream.android.video.generated.models.VideoEvent(), io.getstream.android.video.generated.models.WSCallEvent {

    override fun getEventType(): kotlin.String {
        return type
    }

    override fun getCallCID(): kotlin.String {
        return callCid
    }
}
