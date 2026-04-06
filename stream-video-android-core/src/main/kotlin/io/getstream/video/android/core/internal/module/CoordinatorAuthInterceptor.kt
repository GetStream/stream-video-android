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

import io.getstream.video.android.core.socket.common.token.TokenRepository
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * CoordinatorAuthInterceptor adds the token authentication to the API calls
 */
internal class CoordinatorAuthInterceptor(
    var apiKey: String,
    val tokenRepository: TokenRepository,
    var authType: String = "jwt",
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val updatedUrl = if (original.url.toString().contains("video")) {
            original.url.newBuilder()
                .addQueryParameter(API_KEY, apiKey)
                .build()
        } else {
            original.url
        }

        val token = tokenRepository.getToken()
        val requestBuilder = original.newBuilder()
            .url(updatedUrl)
            .header(STREAM_AUTH_TYPE, authType)

        // Only add the Authorization header when a token is present.
        // For guest and anonymous users the token starts empty, so omitting it
        // prevents a spurious "Authorization: " header that the server rejects.
        if (token.isNotBlank()) {
            requestBuilder.addHeader(HEADER_AUTHORIZATION, token)
        }

        val updated = requestBuilder.build()

        return chain.proceed(updated)
    }

    private companion object {
        /**
         * Query key used to authenticate to the API.
         */
        private const val API_KEY = "api_key"
        private const val STREAM_AUTH_TYPE = "stream-auth-type"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
}
