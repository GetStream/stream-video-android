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
import org.threeten.bp.OffsetDateTime

/**
 * This event is sent when a user requests access to a feature on a call, clients receiving this event should display a permission request to the user
 *
 * @param callCid * @param createdAt * @param permissions The list of permissions requested by the user
 * @param type The type of event: \"call.permission_request\" in this case
 * @param user */

data class PermissionRequestEvent(

    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: OffsetDateTime,

    /* The list of permissions requested by the user */
    @Json(name = "permissions")
    val permissions: kotlin.collections.List<kotlin.String>,

    /* The type of event: \"call.permission_request\" in this case */
    @Json(name = "type")
    val type: kotlin.String,

    @Json(name = "user")
    val user: UserResponse

)
