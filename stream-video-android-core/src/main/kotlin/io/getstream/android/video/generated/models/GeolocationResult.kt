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

/**
 *
 */

data class GeolocationResult(
    @Json(name = "accuracy_radius")
    val accuracyRadius: kotlin.Int,

    @Json(name = "city")
    val city: kotlin.String,

    @Json(name = "continent")
    val continent: kotlin.String,

    @Json(name = "continent_code")
    val continentCode: kotlin.String,

    @Json(name = "country")
    val country: kotlin.String,

    @Json(name = "country_iso_code")
    val countryIsoCode: kotlin.String,

    @Json(name = "latitude")
    val latitude: kotlin.Float,

    @Json(name = "longitude")
    val longitude: kotlin.Float,

    @Json(name = "subdivision")
    val subdivision: kotlin.String,

    @Json(name = "subdivision_iso_code")
    val subdivisionIsoCode: kotlin.String,
)
