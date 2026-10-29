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
public data class EncodingProfile (
    @Json(name = "getstats_snapshots")
    public val getstatsSnapshots: Int,

    @Json(name = "source_file")
    public val sourceFile: String,

    @Json(name = "svc_modes")
    public val svcModes: List<String> = emptyList(),

    @Json(name = "quality_limitation_durations_s")
    public val qualityLimitationDurationsS: Map<String, Int> = emptyMap(),

    @Json(name = "quality_limitation_samples")
    public val qualityLimitationSamples: Map<String, Int> = emptyMap(),

    @Json(name = "avg_send_kbps")
    public val avgSendKbps: Int? = null,

    @Json(name = "codec")
    public val codec: String? = null,

    @Json(name = "encoder_impl")
    public val encoderImpl: String? = null,

    @Json(name = "fps_p10")
    public val fpsP10: Int? = null,

    @Json(name = "fps_p50")
    public val fpsP50: Int? = null,

    @Json(name = "hardware_encode")
    public val hardwareEncode: Boolean? = null,

    @Json(name = "ladder_type")
    public val ladderType: String? = null,

    @Json(name = "power_efficient")
    public val powerEfficient: Boolean? = null,

    @Json(name = "resolution")
    public val resolution: String? = null,

    @Json(name = "simulcast_layers")
    public val simulcastLayers: Int? = null
)
