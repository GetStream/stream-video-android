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

import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.UserRequest




import com.squareup.moshi.Json

/**
 *
 *
 * @param createdBy
 * @param createdById
 * @param custom
 * @param members
 * @param settingsOverride
 * @param startsAt
 * @param team
 */


data class CallRequest (

    @Json(name = "created_by")
    val createdBy: UserRequest? = null,

    @Json(name = "created_by_id")
    val createdById: kotlin.String? = null,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,

    @Json(name = "members")
    val members: kotlin.collections.List<MemberRequest>? = null,

    @Json(name = "settings_override")
    val settingsOverride: CallSettingsRequest? = null,

    @Json(name = "starts_at")
    val startsAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "team")
    val team: kotlin.String? = null

)
