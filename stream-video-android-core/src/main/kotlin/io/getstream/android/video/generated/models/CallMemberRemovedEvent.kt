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

/**
 * This event is sent when one or more members are removed from a call
 */

data class CallMemberRemovedEvent(
    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "call")
    val call: io.getstream.android.video.generated.models.CallResponse,

    @Json(name = "type")
    val type: kotlin.String = "call.member_removed",

    @Json(name = "members")
    val members: kotlin.collections.List<kotlin.String> = emptyList()
)
: io.getstream.android.video.generated.models.VideoEvent(), io.getstream.android.video.generated.models.WSCallEvent
{
    
    override fun getEventType(): kotlin.String {
        return type
    }

    override fun getCallCID(): kotlin.String {
        return callCid
    }
}
