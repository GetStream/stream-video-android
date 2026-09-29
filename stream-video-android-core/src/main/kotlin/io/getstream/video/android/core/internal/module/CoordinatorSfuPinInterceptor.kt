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

package io.getstream.video.android.core.internal.module

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `?sfu_id=` on coordinator join requests when a local-dev pin is configured.
 *
 * The coordinator reads this query via `WithPinToSFUID`. It is not part of the
 * published OpenAPI join body, so this interceptor keeps
 * [io.getstream.android.video.generated.apis.ProductvideoApi.joinCall]
 * binary-compatible instead of adding a Retrofit `@Query`.
 */
internal class CoordinatorSfuPinInterceptor(
    private val pinnedSfuId: String?,
) : Interceptor {
    companion object {
        const val QUERY_SFU_ID = "sfu_id"
        const val JOIN_PATH_SUFFIX = "/join"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val pin = pinnedSfuId?.takeIf { it.isNotBlank() }
        if (pin == null ||
            !request.url.encodedPath.endsWith(JOIN_PATH_SUFFIX) ||
            request.url.queryParameter(QUERY_SFU_ID) != null
        ) {
            return chain.proceed(request)
        }
        val url = request.url.newBuilder()
            .addQueryParameter(QUERY_SFU_ID, pin)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
