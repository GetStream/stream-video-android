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
 * 
 */

data class JoinCallRequest (
    @Json(name = "location")
    val location: kotlin.String,

    @Json(name = "create")
    val create: kotlin.Boolean? = null,

    @Json(name = "members_limit")
    val membersLimit: kotlin.Int? = null,

    @Json(name = "migrating_from")
    val migratingFrom: kotlin.String? = null,

    @Json(name = "notify")
    val notify: kotlin.Boolean? = null,

    @Json(name = "ring")
    val ring: kotlin.Boolean? = null,

    @Json(name = "video")
    val video: kotlin.Boolean? = null,

    @Json(name = "data")
    val data: io.getstream.android.video.generated.models.CallRequest? = null
)
