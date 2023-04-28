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
 *
 *
 * @param connectionId The connection_id for this client
 * @param createdAt
 * @param type The type of event: \"health.check\" in this case
 */


data class HealthCheckEvent (

    /* The connection_id for this client */
    @Json(name = "connection_id")
    val connectionId: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    /* The type of event: \"health.check\" in this case */
    @Json(name = "type")
    val type: kotlin.String = "health.check"

) : VideoEvent(), WSClientEvent {

    override fun getEventType(): String {
        return type
    }
}
