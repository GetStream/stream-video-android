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

package io.getstream.video.android.core.socket.common.parser2

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.getstream.android.video.generated.infrastructure.BigDecimalAdapter
import io.getstream.android.video.generated.infrastructure.BigIntegerAdapter
import io.getstream.android.video.generated.infrastructure.ByteArrayAdapter
import io.getstream.android.video.generated.infrastructure.LocalDateAdapter
import io.getstream.android.video.generated.infrastructure.LocalDateTimeAdapter
import io.getstream.android.video.generated.infrastructure.OffsetDateTimeAdapter
import io.getstream.android.video.generated.infrastructure.URIAdapter
import io.getstream.android.video.generated.infrastructure.UUIDAdapter
import io.getstream.android.video.generated.models.VideoEventAdapter
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.parser2.adapters.DateAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal class MoshiVideoParser : VideoParser {

    private val moshi: Moshi by lazy {
        Builder()
            // Infrastructure @ToJson @FromJson
            .add(OffsetDateTimeAdapter()).add(LocalDateTimeAdapter()).add(LocalDateAdapter())
            .add(UUIDAdapter()).add(ByteArrayAdapter()).add(URIAdapter()).add(BigDecimalAdapter())
            .add(BigIntegerAdapter())
            // JsonAdapter
            .addAdapter(DateAdapter())
            .add(lenientAdapter(VideoEventAdapter())).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.AudioSettingsRequest.DefaultDevice.DefaultDeviceAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.AudioSettingsResponse.DefaultDevice.DefaultDeviceAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.CreateDeviceRequest.PushProvider.PushProviderAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.NoiseCancellationSettings.Mode.ModeAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.OwnCapability.OwnCapabilityAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.RecordSettingsRequest.Mode.ModeAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.RecordSettingsRequest.Quality.QualityAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.TranscriptionSettingsRequest.Mode.ModeAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.TranscriptionSettingsResponse.Mode.ModeAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.TranscriptionSettingsResponse.ClosedCaptionMode.ClosedCaptionModeAdapter(),
                ),
            )
            .add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.VideoSettingsRequest.CameraFacing.CameraFacingAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.VideoSettingsResponse.CameraFacing.CameraFacingAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.LayoutSettingsRequest.Name.NameAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.RTMPBroadcastRequest.Quality.QualityAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.RTMPSettingsRequest.Quality.QualityAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.TranscriptionSettingsRequest.ClosedCaptionMode.ClosedCaptionModeAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.TranscriptionSettingsRequest.Language.LanguageAdapter(),
                ),
            ).add(
                lenientAdapter(
                    io.getstream.android.video.generated.models.TranscriptionSettingsResponse.Language.LanguageAdapter(),
                ),
            )
            // Factories
            .addLast(KotlinJsonAdapterFactory()).build()
    }

    private inline fun <reified T> Moshi.Builder.addAdapter(adapter: JsonAdapter<T>) = apply {
        this.add(T::class.java, lenientAdapter(adapter))
    }

    override fun configRetrofit(builder: Retrofit.Builder): Retrofit.Builder {
        return builder.addConverterFactory(MoshiConverterFactory.create(moshi).withErrorLogging())
    }

    override fun toJson(any: Any): String = when {
        Map::class.java.isAssignableFrom(any.javaClass) -> serializeMap(any)
        else -> moshi.adapter(any.javaClass).toJson(any)
    }

    private val mapAdapter = moshi.adapter(Map::class.java)

    private fun serializeMap(any: Any): String {
        return mapAdapter.toJson(any as Map<*, *>)
    }

    override fun <T : Any> fromJson(raw: String, clazz: Class<T>): T {
        return moshi.adapter(clazz).fromJson(raw)!!
    }
}

// Make any adapter lenient
inline fun <reified T> lenientAdapter(adapter: JsonAdapter<T>): JsonAdapter<T> {
    return object : JsonAdapter<T>() {

        @ToJson
        override fun toJson(writer: JsonWriter, value: T?) {
            writer.isLenient = true
            adapter.toJson(writer, value)
        }

        @FromJson
        override fun fromJson(reader: JsonReader): T? {
            reader.isLenient = true
            return adapter.fromJson(reader)
        }
    }
}
