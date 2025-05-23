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

package io.getstream.video.android.core.interceptor

import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient

object StreamOkhttpInterceptorRegistry {
    private val interceptors = mutableListOf<Interceptor>()

    @ExperimentalStreamVideoApi
    fun register(interceptor: Interceptor) {
        interceptors += interceptor
    }

    internal fun applyTo(builder: OkHttpClient.Builder) {
        interceptors.forEach { builder.addInterceptor(it) }
    }
}
