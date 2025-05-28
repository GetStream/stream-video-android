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
    private val authType: () -> String,
) : Interceptor {

    private val logger by taggedLogger("Call:TokenAuthInterceptor")

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
            val format = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
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