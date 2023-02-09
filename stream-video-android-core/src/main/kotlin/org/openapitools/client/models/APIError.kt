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
 * @param statusCode Response HTTP status code
 * @param code API error code
 * @param details Additional error-specific information
 * @param duration Request duration
 * @param exceptionFields Additional error info
 * @param message Message describing an error
 * @param moreInfo URL with additional information
 */

internal data class APIError(

    /* Response HTTP status code */
    @Json(name = "StatusCode")
    val statusCode: java.math.BigDecimal? = null,

    /* API error code */
    @Json(name = "code")
    val code: java.math.BigDecimal? = null,

    /* Additional error-specific information */
    @Json(name = "details")
    val details: kotlin.collections.List<java.math.BigDecimal>? = null,

    /* Request duration */
    @Json(name = "duration")
    val duration: kotlin.String? = null,

    /* Additional error info */
    @Json(name = "exception_fields")
    val exceptionFields: kotlin.collections.Map<kotlin.String, kotlin.String>? = null,

    /* Message describing an error */
    @Json(name = "message")
    val message: kotlin.String? = null,

    /* URL with additional information */
    @Json(name = "more_info")
    val moreInfo: kotlin.String? = null

)
