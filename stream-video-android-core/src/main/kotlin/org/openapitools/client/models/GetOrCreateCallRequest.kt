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

import org.openapitools.client.models.CallRequest




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 *
 *
 * @param `data`
 * @param membersLimit
 * @param notify if provided it sends a notification event to the members for this call
 * @param ring if provided it sends a ring event to the members for this call
 */


data class GetOrCreateCallRequest (

    @Json(name = "data")
    val `data`: CallRequest? = null,

    @Json(name = "members_limit")
    val membersLimit: kotlin.Int? = null,

    /* if provided it sends a notification event to the members for this call */
    @Json(name = "notify")
    val notify: kotlin.Boolean? = null,

    /* if provided it sends a ring event to the members for this call */
    @Json(name = "ring")
    val ring: kotlin.Boolean? = null

)
