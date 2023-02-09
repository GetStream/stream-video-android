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
 * @param call * @param credentials * @param duration Duration of the request in human-readable format
 * @param members * @param membership */

internal data class GetCallEdgeServerResponse(

    @Json(name = "call")
    val call: CallResponse,

    @Json(name = "credentials")
    val credentials: Credentials,

    /* Duration of the request in human-readable format */
    @Json(name = "duration")
    val duration: kotlin.String? = null,

    @Json(name = "members")
    val members: kotlin.collections.List<MemberResponse>? = null,

    @Json(name = "membership")
    val membership: MemberResponse? = null

)
