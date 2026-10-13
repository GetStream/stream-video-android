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
 * This event is sent when call recording has stopped
 */
data class CallRecordingStoppedEvent (
    @Json(name = "call_cid")
    val callCid: String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "egress_id")
    val egressId: String,

    @Json(name = "recording_type")
    val recordingType: RecordingType,

    @Json(name = "type")
    val type: String = "call.recording_stopped"
)
: io.getstream.android.video.api.generated.models.VideoEvent(), io.getstream.android.video.api.generated.models.WSCallEvent
{
    
    override fun getEventType(): String {
        return type
    }

    override fun getCallCID(): String {
        return callCid
    }
    
    /**
    * RecordingType Enum
    */
    sealed class RecordingType(val value: String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: String): RecordingType = when (s) {
                    "composite" -> Composite
                    "individual" -> Individual
                    "raw" -> Raw
                    else -> Unknown(s)
                }
            }
            object Composite : RecordingType("composite")
            object Individual : RecordingType("individual")
            object Raw : RecordingType("raw")
            data class Unknown(val unknownValue: String) : RecordingType(unknownValue)
        

        class RecordingTypeAdapter : JsonAdapter<RecordingType>() {
            @FromJson
            override fun fromJson(reader: JsonReader): RecordingType? {
                val s = reader.nextString() ?: return null
                return RecordingType.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: RecordingType?) {
                writer.value(value?.value)
            }
        }
    }    
}
