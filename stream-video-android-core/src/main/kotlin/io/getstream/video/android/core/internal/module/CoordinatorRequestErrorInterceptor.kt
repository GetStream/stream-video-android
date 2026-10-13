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
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HTTP
import retrofit2.http.OPTIONS
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

private object CoordinatorRequestErrorMessage {
    const val DNS_LOOKUP_FAILED: String = "DNS lookup failed"
    const val REQUEST_TIMED_OUT: String = "request timed out"
    const val REQUEST_INTERRUPTED: String = "request interrupted"
    const val CONNECTION_FAILED: String = "connection failed"
    const val TLS_CONNECTION_FAILED: String = "TLS connection failed"
    const val NETWORK_IO_FAILED: String = "network I/O failed"
    const val TIMEOUT_KEYWORD: String = "timeout"
    const val FAILURE_SEPARATOR: String = " failed: "
    const val CAUSE_SEPARATOR: String = " — "
}

/**
 * Adds sanitized request context to coordinator API failures.
 *
 * For HTTP 4xx and 5xx responses, the interceptor adds the endpoint to the OkHttp response message.
 * Retrofit then includes that message when it creates its `HttpException`, producing output such as:
 *
 * `HTTP 404 POST video.stream-io-api.com/video/call/{type}/{id}/join failed: Not Found`
 *
 * The response code and body are not changed, so Retrofit error parsing and authentication retries
 * continue to work normally.
 *
 * Transport failures are wrapped in [CoordinatorRequestException] with a message in this format:
 *
 * `METHOD host/route-template failed: failure-category — original-cause-message`
 *
 * For example:
 *
 * `POST video.stream-io-api.com/video/call/{type}/{id}/join failed: DNS lookup failed — Unable to resolve host`
 *
 * The endpoint comes from the Retrofit HTTP annotation when available, so path parameters remain
 * placeholders. For requests created directly with OkHttp, the URL's encoded path is used instead.
 * Query parameters are always omitted. The original [IOException] is retained as the cause. If it has
 * no usable message, the output ends after the normalized failure category, for example `failed:
 * request timed out`.
 */
internal class CoordinatorRequestErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            val response = chain.proceed(request)
            if (response.code in 400..599) {
                response.newBuilder()
                    .message(
                        request.sanitizedDescription() +
                            CoordinatorRequestErrorMessage.FAILURE_SEPARATOR +
                            response.message,
                    )
                    .build()
            } else {
                response
            }
        } catch (exception: IOException) {
            throw CoordinatorRequestException(request, exception)
        }
    }
}

internal class CoordinatorRequestException(
    val requestDescription: String,
    val failureReason: String,
    cause: IOException,
) : IOException(
    requestDescription + CoordinatorRequestErrorMessage.FAILURE_SEPARATOR + failureReason,
    cause,
) {
    constructor(request: Request, cause: IOException) : this(
        requestDescription = request.sanitizedDescription(),
        failureReason = cause.failureDescription(),
        cause = cause,
    )
}

/**
 * Restores coordinator request context when OkHttp's call timeout replaces the exception thrown by
 * the interceptor with a new [InterruptedIOException].
 */
internal fun IOException.withCoordinatorRequestContext(): IOException {
    if (this is CoordinatorRequestException) return this

    var nestedCause = cause
    while (nestedCause != null && nestedCause !== nestedCause.cause) {
        if (nestedCause is CoordinatorRequestException) {
            return CoordinatorRequestException(
                requestDescription = nestedCause.requestDescription,
                failureReason = failureDescription(),
                cause = this,
            )
        }
        nestedCause = nestedCause.cause
    }
    return this
}

internal fun Request.sanitizedDescription(): String {
    val routeTemplate = tag(Invocation::class.java)
        ?.method()
        ?.annotations
        ?.firstNotNullOfOrNull { annotation ->
            when (annotation) {
                is DELETE -> annotation.value
                is GET -> annotation.value
                is HEAD -> annotation.value
                is HTTP -> annotation.path
                is OPTIONS -> annotation.value
                is PATCH -> annotation.value
                is POST -> annotation.value
                is PUT -> annotation.value
                else -> null
            }
        }
        ?.takeIf { it.isNotBlank() }
    return "$method ${url.host}${routeTemplate ?: url.encodedPath}"
}

private fun IOException.failureDescription(): String {
    val category = when (this) {
        is UnknownHostException -> CoordinatorRequestErrorMessage.DNS_LOOKUP_FAILED
        is SocketTimeoutException -> CoordinatorRequestErrorMessage.REQUEST_TIMED_OUT
        is InterruptedIOException -> {
            if (message?.contains(CoordinatorRequestErrorMessage.TIMEOUT_KEYWORD, ignoreCase = true) == true) {
                CoordinatorRequestErrorMessage.REQUEST_TIMED_OUT
            } else {
                CoordinatorRequestErrorMessage.REQUEST_INTERRUPTED
            }
        }
        is ConnectException -> CoordinatorRequestErrorMessage.CONNECTION_FAILED
        is SSLException -> CoordinatorRequestErrorMessage.TLS_CONNECTION_FAILED
        else -> CoordinatorRequestErrorMessage.NETWORK_IO_FAILED
    }
    return message?.takeIf { it.isNotBlank() }
        ?.let { "$category${CoordinatorRequestErrorMessage.CAUSE_SEPARATOR}$it" }
        ?: category
}
