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
 * @param connectionId * @param `data` * @param datacenterHintedId * @param members * @param ring */

data class JoinCallRequest(

    @Json(name = "connection_id")
    val connectionId: kotlin.String? = null,

    @Json(name = "data")
    val `data`: CallRequest? = null,

    @Json(name = "datacenter_hinted_id")
    val datacenterHintedId: kotlin.String? = null,

    @Json(name = "members")
    val members: PaginationParamsRequest? = null,

    @Json(name = "ring")
    val ring: kotlin.Boolean? = null

)
