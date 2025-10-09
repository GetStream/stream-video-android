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

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * This event is sent when a frame is captured from a call
 */

data class CallFrameRecordingFrameReadyEvent (
    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "captured_at")
    val capturedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "egress_id")
    val egressId: kotlin.String,

    @Json(name = "session_id")
    val sessionId: kotlin.String,

    @Json(name = "track_type")
    val trackType: kotlin.String,

    @Json(name = "url")
    val url: kotlin.String,

    @Json(name = "users")
    val users: kotlin.collections.Map<kotlin.String, io.getstream.android.video.generated.models.UserResponse> = emptyMap(),

    @Json(name = "type")
    val type: kotlin.String
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
