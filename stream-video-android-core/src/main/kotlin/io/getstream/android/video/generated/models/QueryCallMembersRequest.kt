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
import kotlin.collections.Map

/**
 *
 */

data class QueryCallMembersRequest(
    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "type")
    val type: kotlin.String,

    @Json(name = "limit")
    val limit: kotlin.Int? = null,

    @Json(name = "next")
    val next: kotlin.String? = null,

    @Json(name = "prev")
    val prev: kotlin.String? = null,

    @Json(name = "sort")
    val sort: kotlin.collections.List<io.getstream.android.video.generated.models.SortParamRequest>? =
        null,

    @Json(name = "filter_conditions")
    val filterConditions: kotlin.collections.Map<kotlin.String, Any?>? = null,
)
