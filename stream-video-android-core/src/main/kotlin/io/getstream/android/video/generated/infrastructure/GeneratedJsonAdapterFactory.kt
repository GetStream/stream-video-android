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

package io.getstream.android.video.generated.infrastructure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.internal.Util
import java.lang.reflect.Type

/**
 * Resolves Moshi codegen adapters (`@JsonClass(generateAdapter = true)`) and nothing else.
 * [com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory] does this lookup and then falls
 * back to kotlin-reflect. Video has an adapter for every model it parses, so the fallback is
 * unused weight.
 */
internal object GeneratedJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? {
        if (annotations.isNotEmpty()) return null
        return Util.generatedAdapter(moshi, type, Types.getRawType(type))
    }
}
