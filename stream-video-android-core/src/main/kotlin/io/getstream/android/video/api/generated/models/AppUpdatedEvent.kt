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
 * Emitted when app settings are updated
 */
public data class AppUpdatedEvent (
    @Json(name = "created_at")
    public val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "app")
    public val app: io.getstream.android.video.api.generated.models.AppEventResponse,

    @Json(name = "custom")
    public val custom: Map<String, Any?> = emptyMap(),

    @Json(name = "type")
    public val type: String = "app.updated",

    @Json(name = "received_at")
    public val receivedAt: org.threeten.bp.OffsetDateTime? = null
)
: io.getstream.android.video.api.generated.models.VideoEvent()
{
    
    override fun getEventType(): String {
        return type
    }    
}
