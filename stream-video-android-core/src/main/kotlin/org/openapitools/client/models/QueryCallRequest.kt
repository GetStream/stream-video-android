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
 * *
 * @param sort * @param filterConditions * @param limit * @param memberLimit * @param messageLimit * @param next * @param prev * @param watch */

data class QueryCallRequest(

    @Json(name = "sort")
    val sort: kotlin.collections.List<SortParamRequest>,

    @Json(name = "filter_conditions")
    val filterConditions: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,

    @Json(name = "limit")
    val limit: java.math.BigDecimal? = null,

    @Json(name = "member_limit")
    val memberLimit: java.math.BigDecimal? = null,

    @Json(name = "message_limit")
    val messageLimit: java.math.BigDecimal? = null,

    @Json(name = "next")
    val next: kotlin.String? = null,

    @Json(name = "prev")
    val prev: kotlin.String? = null,

    @Json(name = "watch")
    val watch: kotlin.Boolean? = null

)
