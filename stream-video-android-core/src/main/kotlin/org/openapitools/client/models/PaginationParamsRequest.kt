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
 * @param idGt * @param idGte * @param idLt * @param idLte * @param limit * @param offset */

data class PaginationParamsRequest(

    @Json(name = "id_gt")
    val idGt: java.math.BigDecimal? = null,

    @Json(name = "id_gte")
    val idGte: java.math.BigDecimal? = null,

    @Json(name = "id_lt")
    val idLt: java.math.BigDecimal? = null,

    @Json(name = "id_lte")
    val idLte: java.math.BigDecimal? = null,

    @Json(name = "limit")
    val limit: java.math.BigDecimal? = null,

    @Json(name = "offset")
    val offset: java.math.BigDecimal? = null

)
