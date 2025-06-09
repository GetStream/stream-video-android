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

package io.getstream.video.android.core.internal.network

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.socket.common.token.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

internal class TokenAuthInterceptor(
    private val tokenManager: TokenManager,
    private val authType: () -> String = { "jwt" },
) : Interceptor {

    private val logger by taggedLogger("Call:TokenAuthInterceptor")
    private val format by lazy {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        tokenManager.ensureTokenLoaded()
        var response = chain.proceed(chain.request().addTokenHeader())
        if (response.isAuthError()) {
            tokenManager.expireToken()
            tokenManager.loadSync()
            response.close()
            response = chain.proceed(chain.request().addTokenHeader())
        }
        return response
    }

    private fun okhttp3.Request.addTokenHeader(): okhttp3.Request = newBuilder()
        .header(HEADER_AUTHORIZATION, tokenManager.getToken())
        .header(STREAM_AUTH_TYPE, authType())
        .build()

    private fun Response.isAuthError(): Boolean {
        if (isSuccessful) return false
        return try {
            val errorCode = format
                .decodeFromString<ErrorResponse>(peekBody(Long.MAX_VALUE).string())
                .code
            VideoErrorCode.isAuthenticationError(errorCode)
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse error response" }
            false
        }
    }

    private companion object {
        private const val STREAM_AUTH_TYPE = "stream-auth-type"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
}
