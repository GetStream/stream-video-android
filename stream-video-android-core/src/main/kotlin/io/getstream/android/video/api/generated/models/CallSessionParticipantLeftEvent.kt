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
 * This event is sent when a participant leaves a call session
 */
public data class CallSessionParticipantLeftEvent (
    @Json(name = "call_cid")
    public val callCid: String,

    @Json(name = "created_at")
    public val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "duration_seconds")
    public val durationSeconds: Int,

    @Json(name = "session_id")
    public val sessionId: String,

    @Json(name = "participant")
    public val participant: io.getstream.android.video.api.generated.models.CallParticipantResponse,

    @Json(name = "type")
    public val type: String = "call.session_participant_left",

    @Json(name = "reason")
    public val reason: String? = null
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
