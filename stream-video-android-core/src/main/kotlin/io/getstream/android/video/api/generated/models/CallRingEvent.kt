/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.android.video.api.generated.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * This event is sent to all call members to notify they are getting called
 */
public data class CallRingEvent (
    @Json(name = "call_cid")
    public val callCid: String,

    @Json(name = "created_at")
    public val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "session_id")
    public val sessionId: String,

    @Json(name = "video")
    public val video: Boolean,

    @Json(name = "members")
    public val members: List<io.getstream.android.video.api.generated.models.MemberResponse> = emptyList(),

    @Json(name = "call")
    public val call: io.getstream.android.video.api.generated.models.CallResponse,

    @Json(name = "user")
    public val user: io.getstream.android.video.api.generated.models.UserResponse,

    @Json(name = "type")
    public val type: String = "call.ring",

    @Json(name = "ring_id")
    public val ringId: String? = null
)
: io.getstream.android.video.api.generated.models.VideoEvent(), io.getstream.android.video.api.generated.models.WSCallEvent
{
    
    override fun getEventType(): String {
        return type
    }

    override fun getCallCID(): String {
        return callCid
    }    
}
