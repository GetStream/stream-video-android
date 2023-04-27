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

import org.openapitools.client.models.ConnectUserDetailsRequest
import org.openapitools.client.models.DeviceFieldsRequest




import com.squareup.moshi.Json

/**
 *
 *
 * @param token
 * @param userDetails
 * @param device
 */


data class WSAuthMessageRequest (

    @Json(name = "token")
    val token: kotlin.String,

    @Json(name = "user_details")
    val userDetails: ConnectUserDetailsRequest,

    @Json(name = "device")
    val device: DeviceFieldsRequest? = null

)
