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
    "UnusedImport"
)

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 *
 */

data class UserSessionStats (
    @Json(name = "freeze_duration_seconds")
    val freezeDurationSeconds: kotlin.Int,

    @Json(name = "group")
    val group: kotlin.String,

    @Json(name = "max_freeze_fraction")
    val maxFreezeFraction: kotlin.Float,

    @Json(name = "max_freezes_duration_seconds")
    val maxFreezesDurationSeconds: kotlin.Int,

    @Json(name = "min_event_ts")
    val minEventTs: kotlin.Int,

    @Json(name = "packet_loss_fraction")
    val packetLossFraction: kotlin.Float,

    @Json(name = "publisher_packet_loss_fraction")
    val publisherPacketLossFraction: kotlin.Float,

    @Json(name = "publishing_duration_seconds")
    val publishingDurationSeconds: kotlin.Int,

    @Json(name = "quality_score")
    val qualityScore: kotlin.Float,

    @Json(name = "receiving_duration_seconds")
    val receivingDurationSeconds: kotlin.Int,

    @Json(name = "session_id")
    val sessionId: kotlin.String,

    @Json(name = "total_pixels_in")
    val totalPixelsIn: kotlin.Int,

    @Json(name = "total_pixels_out")
    val totalPixelsOut: kotlin.Int,

    @Json(name = "average_connection_time")
    val averageConnectionTime: kotlin.Float? = null,

    @Json(name = "browser")
    val browser: kotlin.String? = null,

    @Json(name = "browser_version")
    val browserVersion: kotlin.String? = null,

    @Json(name = "current_ip")
    val currentIp: kotlin.String? = null,

    @Json(name = "current_sfu")
    val currentSfu: kotlin.String? = null,

    @Json(name = "device_model")
    val deviceModel: kotlin.String? = null,

    @Json(name = "device_version")
    val deviceVersion: kotlin.String? = null,

    @Json(name = "distance_to_sfu_kilometers")
    val distanceToSfuKilometers: kotlin.Float? = null,

    @Json(name = "max_fir_per_second")
    val maxFirPerSecond: kotlin.Float? = null,

    @Json(name = "max_freezes_per_second")
    val maxFreezesPerSecond: kotlin.Float? = null,

    @Json(name = "max_nack_per_second")
    val maxNackPerSecond: kotlin.Float? = null,

    @Json(name = "max_pli_per_second")
    val maxPliPerSecond: kotlin.Float? = null,

    @Json(name = "os")
    val os: kotlin.String? = null,

    @Json(name = "os_version")
    val osVersion: kotlin.String? = null,

    @Json(name = "publisher_noise_cancellation_seconds")
    val publisherNoiseCancellationSeconds: kotlin.Float? = null,

    @Json(name = "publisher_quality_limitation_fraction")
    val publisherQualityLimitationFraction: kotlin.Float? = null,

    @Json(name = "publishing_audio_codec")
    val publishingAudioCodec: kotlin.String? = null,

    @Json(name = "publishing_video_codec")
    val publishingVideoCodec: kotlin.String? = null,

    @Json(name = "receiving_audio_codec")
    val receivingAudioCodec: kotlin.String? = null,

    @Json(name = "receiving_video_codec")
    val receivingVideoCodec: kotlin.String? = null,

    @Json(name = "sdk")
    val sdk: kotlin.String? = null,

    @Json(name = "sdk_version")
    val sdkVersion: kotlin.String? = null,

    @Json(name = "subscriber_video_quality_throttled_duration_seconds")
    val subscriberVideoQualityThrottledDurationSeconds: kotlin.Float? = null,

    @Json(name = "truncated")
    val truncated: kotlin.Boolean? = null,

    @Json(name = "webrtc_version")
    val webrtcVersion: kotlin.String? = null,

    @Json(name = "published_tracks")
    val publishedTracks: kotlin.collections.List<io.getstream.android.video.generated.models.PublishedTrackInfo>? = null,

    @Json(name = "subsessions")
    val subsessions: kotlin.collections.List<io.getstream.android.video.generated.models.Subsession>? = null,

    @Json(name = "geolocation")
    val geolocation: io.getstream.android.video.generated.models.GeolocationResult? = null,

    @Json(name = "jitter")
    val jitter: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "latency")
    val latency: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "max_publishing_video_quality")
    val maxPublishingVideoQuality: io.getstream.android.video.generated.models.VideoQuality? = null,

    @Json(name = "max_receiving_video_quality")
    val maxReceivingVideoQuality: io.getstream.android.video.generated.models.VideoQuality? = null,

    @Json(name = "pub_sub_hints")
    val pubSubHints: io.getstream.android.video.generated.models.MediaPubSubHint? = null,

    @Json(name = "publisher_jitter")
    val publisherJitter: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "publisher_latency")
    val publisherLatency: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "publisher_video_quality_limitation_duration_seconds")
    val publisherVideoQualityLimitationDurationSeconds: kotlin.collections.Map<kotlin.String, kotlin.Float>? = null,

    @Json(name = "subscriber_jitter")
    val subscriberJitter: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "subscriber_latency")
    val subscriberLatency: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "timeline")
    val timeline: io.getstream.android.video.generated.models.CallTimeline? = null
)
