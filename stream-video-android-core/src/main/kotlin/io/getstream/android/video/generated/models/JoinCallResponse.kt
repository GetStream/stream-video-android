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
import kotlin.collections.List

/**
 *
 */

data class JoinCallResponse(
    @Json(name = "created")
    val created: kotlin.Boolean,

    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "members")
    val members:
    kotlin.collections.List<io.getstream.android.video.generated.models.MemberResponse>,

    @Json(name = "own_capabilities")
    val ownCapabilities:
    kotlin.collections.List<io.getstream.android.video.generated.models.OwnCapability>,

    @Json(name = "call")
    val call: io.getstream.android.video.generated.models.CallResponse,

    @Json(name = "credentials")
    val credentials: io.getstream.android.video.generated.models.Credentials,

    @Json(name = "stats_options")
    val statsOptions: io.getstream.android.video.generated.models.StatsOptions,

    @Json(name = "membership")
    val membership: io.getstream.android.video.generated.models.MemberResponse? = null,
)
