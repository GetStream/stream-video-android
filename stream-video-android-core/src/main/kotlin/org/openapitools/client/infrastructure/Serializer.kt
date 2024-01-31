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

package org.openapitools.client.infrastructure

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.openapitools.client.models.VideoEventAdapter

object Serializer {
    @JvmStatic
    val moshiBuilder: Moshi.Builder = Moshi.Builder()
        .add(OffsetDateTimeAdapter())
        .add(LocalDateTimeAdapter())
        .add(LocalDateAdapter())
        .add(UUIDAdapter())
        .add(ByteArrayAdapter())
        .add(URIAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .add(VideoEventAdapter())
        .add(org.openapitools.client.models.AudioSettings.DefaultDevice.DefaultDeviceAdapter())
        .add(org.openapitools.client.models.AudioSettingsRequest.DefaultDevice.DefaultDeviceAdapter())
        .add(org.openapitools.client.models.CreateDeviceRequest.PushProvider.PushProviderAdapter())
        .add(org.openapitools.client.models.OwnCapability.OwnCapabilityAdapter())
        .add(org.openapitools.client.models.RecordSettings.Mode.ModeAdapter())
        .add(org.openapitools.client.models.RecordSettings.Quality.QualityAdapter())
        .add(org.openapitools.client.models.RecordSettingsRequest.Mode.ModeAdapter())
        .add(org.openapitools.client.models.RecordSettingsRequest.Quality.QualityAdapter())
        .add(org.openapitools.client.models.TranscriptionSettings.Mode.ModeAdapter())
        .add(org.openapitools.client.models.TranscriptionSettingsRequest.Mode.ModeAdapter())
        .add(org.openapitools.client.models.VideoSettings.CameraFacing.CameraFacingAdapter())
        .add(org.openapitools.client.models.VideoSettingsRequest.CameraFacing.CameraFacingAdapter())
        .add(BigDecimalAdapter())
        .add(BigIntegerAdapter())

    @JvmStatic
    val moshi: Moshi by lazy {
        moshiBuilder.build()
    }
}
