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

package org.openapitools.client.infrastructure

import com.squareup.moshi.Moshi

object Serializer {
    @JvmStatic
    val moshiBuilder: Moshi.Builder = Moshi.Builder()
        .add(org.openapitools.client.models.AudioSettingsRequest.DefaultDevice.DefaultDeviceAdapter())
        .add(org.openapitools.client.models.AudioSettingsResponse.DefaultDevice.DefaultDeviceAdapter())
        .add(org.openapitools.client.models.CreateDeviceRequest.PushProvider.PushProviderAdapter())
        .add(org.openapitools.client.models.LayoutSettingsRequest.Name.NameAdapter())
        .add(org.openapitools.client.models.NoiseCancellationSettings.Mode.ModeAdapter())
        .add(org.openapitools.client.models.OwnCapability.OwnCapabilityAdapter())
        .add(org.openapitools.client.models.RTMPBroadcastRequest.Quality.QualityAdapter())
        .add(org.openapitools.client.models.RTMPSettingsRequest.Quality.QualityAdapter())
        .add(org.openapitools.client.models.RecordSettingsRequest.Mode.ModeAdapter())
        .add(org.openapitools.client.models.RecordSettingsRequest.Quality.QualityAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsRequest.Mode.ModeAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsRequest.ClosedCaptionMode.ClosedCaptionModeAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsRequest.Language.LanguageAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsResponse.ClosedCaptionMode.ClosedCaptionModeAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsResponse.Language.LanguageAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsResponse.Mode.ModeAdapter())
        .add(org.openapitools.client.models.VideoSettingsRequest.CameraFacing.CameraFacingAdapter())
        .add(org.openapitools.client.models.VideoSettingsResponse.CameraFacing.CameraFacingAdapter())
        .add(org.openapitools.client.infrastructure.BigDecimalAdapter())
        .add(org.openapitools.client.infrastructure.BigIntegerAdapter())
        .add(org.openapitools.client.infrastructure.ByteArrayAdapter())
        .add(org.openapitools.client.infrastructure.LocalDateAdapter())
        .add(org.openapitools.client.infrastructure.LocalDateTimeAdapter())
        .add(org.openapitools.client.infrastructure.OffsetDateTimeAdapter())
        .add(org.openapitools.client.infrastructure.URIAdapter())
        .add(org.openapitools.client.infrastructure.UUIDAdapter())

    @JvmStatic
    val moshi: Moshi by lazy {
        moshiBuilder.build()
    }
}
