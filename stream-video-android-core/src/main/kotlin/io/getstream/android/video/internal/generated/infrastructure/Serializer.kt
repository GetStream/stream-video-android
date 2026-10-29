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

package io.getstream.android.video.internal.generated.infrastructure

import com.squareup.moshi.Moshi

public object Serializer {
    @JvmStatic
    public val moshiBuilder: Moshi.Builder = Moshi.Builder()
        .add(io.getstream.android.video.api.generated.models.AudioSettingsRequest.DefaultDevice.DefaultDeviceAdapter())
        .add(io.getstream.android.video.api.generated.models.AudioSettingsResponse.DefaultDevice.DefaultDeviceAdapter())
        .add(io.getstream.android.video.api.generated.models.CallRecordingFailedEvent.RecordingType.RecordingTypeAdapter())
        .add(io.getstream.android.video.api.generated.models.CallRecordingReadyEvent.RecordingType.RecordingTypeAdapter())
        .add(io.getstream.android.video.api.generated.models.CallRecordingStartedEvent.RecordingType.RecordingTypeAdapter())
        .add(io.getstream.android.video.api.generated.models.CallRecordingStoppedEvent.RecordingType.RecordingTypeAdapter())
        .add(io.getstream.android.video.api.generated.models.CreateDeviceRequest.PushProvider.PushProviderAdapter())
        .add(io.getstream.android.video.api.generated.models.EncryptionSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.EncryptionSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.FrameRecordingSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.FrameRecordingSettingsRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.api.generated.models.FrameRecordingSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.IndividualRecordingSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.IndividualRecordingSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.IngressAudioEncodingOptionsRequest.Channels.ChannelsAdapter())
        .add(io.getstream.android.video.api.generated.models.IngressSourceRequest.Fps.FpsAdapter())
        .add(io.getstream.android.video.api.generated.models.IngressVideoLayerRequest.Codec.CodecAdapter())
        .add(io.getstream.android.video.api.generated.models.LayoutSettingsRequest.Name.NameAdapter())
        .add(io.getstream.android.video.api.generated.models.LifecycleSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.LifecycleSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.NoiseCancellationSettings.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.OwnCapability.OwnCapabilityAdapter())
        .add(io.getstream.android.video.api.generated.models.RTMPBroadcastRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.api.generated.models.RTMPSettingsRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.api.generated.models.RawRecordingSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.RawRecordingSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.RecordSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.RecordSettingsRequest.Quality.QualityAdapter())
        .add(io.getstream.android.video.api.generated.models.StartClosedCaptionsRequest.Language.LanguageAdapter())
        .add(io.getstream.android.video.api.generated.models.StartTranscriptionRequest.Language.LanguageAdapter())
        .add(io.getstream.android.video.api.generated.models.TranscriptionSettingsRequest.ClosedCaptionMode.ClosedCaptionModeAdapter())
        .add(io.getstream.android.video.api.generated.models.TranscriptionSettingsRequest.Language.LanguageAdapter())
        .add(io.getstream.android.video.api.generated.models.TranscriptionSettingsRequest.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.TranscriptionSettingsResponse.ClosedCaptionMode.ClosedCaptionModeAdapter())
        .add(io.getstream.android.video.api.generated.models.TranscriptionSettingsResponse.Language.LanguageAdapter())
        .add(io.getstream.android.video.api.generated.models.TranscriptionSettingsResponse.Mode.ModeAdapter())
        .add(io.getstream.android.video.api.generated.models.VideoSettingsRequest.CameraFacing.CameraFacingAdapter())
        .add(io.getstream.android.video.api.generated.models.VideoSettingsResponse.CameraFacing.CameraFacingAdapter())
        .add(io.getstream.android.video.api.generated.models.VideoEventAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.BigDecimalAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.BigIntegerAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.ByteArrayAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.URIAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.UUIDAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.OffsetDateTimeAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.LocalDateAdapter())
        .add(io.getstream.android.video.internal.generated.infrastructure.LocalDateTimeAdapter())
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
    
    @JvmStatic
    public val moshi: Moshi by lazy {
        moshiBuilder.build()
    }
}