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

package io.getstream.android.video.internal.generated.infrastructure

import com.squareup.moshi.Moshi
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Encodes query parameters the way the API reads them: a [JsonQuery] as one JSON string, where Retrofit would send a
 * list as repeated values, and a [Date] as an RFC 3339 timestamp instead of its `toString()`.
 */
public class QueryConverterFactory(private val moshi: Moshi) : Converter.Factory() {

    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, String>? = when (getRawType(type)) {
        JsonQuery::class.java -> {
            val valueType = (type as? ParameterizedType)?.let { getParameterUpperBound(0, it) } ?: Any::class.java
            val adapter = moshi.adapter<Any>(valueType)
            Converter<JsonQuery<*>, String> { query -> adapter.toJson(query.value) }
        }
        Date::class.java -> Converter<Date, String> { date ->
            SimpleDateFormat(RFC_3339_PATTERN, Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(date)
        }
        else -> null
    }

    private companion object {
        const val RFC_3339_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    }
}
