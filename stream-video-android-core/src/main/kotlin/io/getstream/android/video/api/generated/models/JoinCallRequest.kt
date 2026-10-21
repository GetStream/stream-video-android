/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.android.video.api.generated.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * 
 */
public data class JoinCallRequest (
    @Json(name = "location")
    public val location: String,

    @Json(name = "create")
    public val create: Boolean? = null,

    @Json(name = "e2ee")
    public val e2ee: Boolean? = null,

    @Json(name = "hint_high_scale_livestream_publisher")
    public val hintHighScaleLivestreamPublisher: Boolean? = null,

    @Json(name = "members_limit")
    public val membersLimit: Int? = null,

    @Json(name = "migrating_from")
    public val migratingFrom: String? = null,

    @Json(name = "notify")
    public val notify: Boolean? = null,

    @Json(name = "ring")
    public val ring: Boolean? = null,

    @Json(name = "video")
    public val video: Boolean? = null,

    @Json(name = "migrating_from_list")
    public val migratingFromList: List<String>? = null,

    @Json(name = "data")
    public val data: io.getstream.android.video.api.generated.models.CallRequest? = null
)
