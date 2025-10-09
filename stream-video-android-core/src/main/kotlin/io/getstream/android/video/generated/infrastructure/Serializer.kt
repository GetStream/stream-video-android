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

package io.getstream.android.video.generated.infrastructure

import com.squareup.moshi.Moshi

object Serializer {
    @JvmStatic
    val moshiBuilder: Moshi.Builder = Moshi.Builder()
        .add(io.getstream.android.video.generated.models.AudioSettingsRequest.DefaultDevice.DefaultDeviceAdapter())
        .add(io.getstream.android.video.generated.models.AudioSettingsResponse.DefaultDevice.DefaultDeviceAdapter())
        .add(io.getstream.android.video.generated.models.CreateDeviceRequest.PushProvider.PushProviderAdapter())
        .add(io.getstream.android.video.generated.models.FrameRecordingSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.generated.models.FrameRecordingSettingsRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.generated.models.FrameRecordingSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.generated.models.IngressAudioEncodingOptionsRequest.Channels.ChannelsAdapter())
        .add(io.getstream.android.video.generated.models.IngressSourceRequest.Fps.FpsAdapter())
        .add(io.getstream.android.video.generated.models.IngressVideoLayerRequest.Codec.CodecAdapter())
        .add(io.getstream.android.video.generated.models.LayoutSettingsRequest.Name.NameAdapter())
        .add(io.getstream.android.video.generated.models.NoiseCancellationSettings.Mode.ModeAdapter())
        .add(io.getstream.android.video.generated.models.OwnCapability.OwnCapabilityAdapter())
        .add(io.getstream.android.video.generated.models.RTMPBroadcastRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.generated.models.RTMPSettingsRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.generated.models.RecordSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.generated.models.RecordSettingsRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.generated.models.StartClosedCaptionsRequest.Language.LanguageAdapter())
        .add(io.getstream.android.video.generated.models.StartTranscriptionRequest.Language.LanguageAdapter())
        .add(io.getstream.android.video.generated.models.TranscriptionSettingsRequest.ClosedCaptionMode.ClosedCaptionModeAdapter())
        .add(io.getstream.android.video.generated.models.TranscriptionSettingsRequest.Language.LanguageAdapter())
        .add(io.getstream.android.video.generated.models.TranscriptionSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.generated.models.TranscriptionSettingsResponse.ClosedCaptionMode.ClosedCaptionModeAdapter())
        .add(io.getstream.android.video.generated.models.TranscriptionSettingsResponse.Language.LanguageAdapter())
        .add(io.getstream.android.video.generated.models.TranscriptionSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.generated.models.VideoSettingsRequest.CameraFacing.CameraFacingAdapter())
        .add(io.getstream.android.video.generated.models.VideoSettingsResponse.CameraFacing.CameraFacingAdapter())
        .add(io.getstream.android.video.generated.models.VideoEventAdapter())
        .add(io.getstream.android.video.generated.infrastructure.BigDecimalAdapter())
        .add(io.getstream.android.video.generated.infrastructure.BigIntegerAdapter())
        .add(io.getstream.android.video.generated.infrastructure.ByteArrayAdapter())
        .add(io.getstream.android.video.generated.infrastructure.URIAdapter())
        .add(io.getstream.android.video.generated.infrastructure.UUIDAdapter())
        .add(io.getstream.android.video.generated.infrastructure.OffsetDateTimeAdapter())
        .add(io.getstream.android.video.generated.infrastructure.LocalDateAdapter())
        .add(io.getstream.android.video.generated.infrastructure.LocalDateTimeAdapter())
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
    
    @JvmStatic
    val moshi: Moshi by lazy {
        moshiBuilder.build()
    }
}