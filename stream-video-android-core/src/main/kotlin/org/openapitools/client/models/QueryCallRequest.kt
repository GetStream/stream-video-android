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
 * @param sort * @param clientId * @param connectionId * @param custom * @param filterConditions * @param id * @param image * @param limit * @param memberLimit * @param messageLimit * @param name * @param next * @param prev * @param role * @param teams * @param watch */

internal data class QueryCallRequest(

    @Json(name = "sort")
    val sort: kotlin.collections.List<SortParamRequest>,

    @Json(name = "client_id")
    val clientId: kotlin.String? = null,

    @Json(name = "connection_id")
    val connectionId: kotlin.String? = null,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,

    @Json(name = "filter_conditions")
    val filterConditions: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,

    @Json(name = "id")
    val id: kotlin.String? = null,

    @Json(name = "image")
    val image: kotlin.String? = null,

    @Json(name = "limit")
    val limit: java.math.BigDecimal? = null,

    @Json(name = "member_limit")
    val memberLimit: java.math.BigDecimal? = null,

    @Json(name = "message_limit")
    val messageLimit: java.math.BigDecimal? = null,

    @Json(name = "name")
    val name: kotlin.String? = null,

    @Json(name = "next")
    val next: kotlin.String? = null,

    @Json(name = "prev")
    val prev: kotlin.String? = null,

    @Json(name = "role")
    val role: kotlin.String? = null,

    @Json(name = "teams")
    val teams: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "watch")
    val watch: kotlin.Boolean? = null

)
