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

package org.openapitools.client.models





import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * Represents custom chat command
 *
 * @param args Arguments help text, shown in commands auto-completion
 * @param description Description, shown in commands auto-completion
 * @param name Unique command name
 * @param set Set name used for grouping commands
 * @param createdAt Date/time of creation
 * @param updatedAt Date/time of the last update
 */


data class Command (

    /* Arguments help text, shown in commands auto-completion */
    @Json(name = "args")
    val args: kotlin.String,

    /* Description, shown in commands auto-completion */
    @Json(name = "description")
    val description: kotlin.String,

    /* Unique command name */
    @Json(name = "name")
    val name: kotlin.String,

    /* Set name used for grouping commands */
    @Json(name = "set")
    val set: kotlin.String,

    /* Date/time of creation */
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime? = null,

    /* Date/time of the last update */
    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime? = null

)
