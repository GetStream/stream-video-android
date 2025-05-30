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
import kotlin.collections.Map

/**
 * CallRequest is the payload for creating a call.
 */

data class CallRequest(
    @Json(name = "starts_at")
    val startsAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "team")
    val team: kotlin.String? = null,

    @Json(name = "video")
    val video: kotlin.Boolean? = null,

    @Json(name = "members")
    val members: kotlin.collections.List<io.getstream.android.video.generated.models.MemberRequest>? =
        null,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, Any?>? = null,

    @Json(name = "settings_override")
    val settingsOverride: io.getstream.android.video.generated.models.CallSettingsRequest? = null,
)
