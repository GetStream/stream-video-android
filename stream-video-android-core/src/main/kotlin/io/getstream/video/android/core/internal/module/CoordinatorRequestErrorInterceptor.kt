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

    val ALLOWED_QUERY_PARAMETER_NAMES: Set<String> = setOf(
        "call_session_id",
        "connection_id",
        "session_id",
        "sfu_id",
    )
}

/**
 * Adds sanitized request context to coordinator API failures.
 *
 * For HTTP 4xx and 5xx responses, the interceptor adds the endpoint to the OkHttp response message.
 * Retrofit then includes that message when it creates its `HttpException`, producing output such as:
 *
 * `HTTP 404 POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: Not Found`
 *
 * The response code and body are not changed, so Retrofit error parsing and authentication retries
 * continue to work normally.
 *
 * Transport failures are wrapped in [CoordinatorRequestException] with a message in this format:
 *
 * `METHOD host/path?query failed: normalized-reason — original-message`
 *
 * Complete transport-error examples:
 *
 * - [UnknownHostException]:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: DNS lookup failed — Unable to resolve host video.stream-io-api.com`
 * - [SocketTimeoutException]:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: request timed out`
 * - [InterruptedIOException] whose message contains `timeout`:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: request timed out`
 * - Any other [InterruptedIOException]:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: request interrupted — Canceled`
 * - [ConnectException]:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: connection failed — Failed to connect to video.stream-io-api.com`
 * - [SSLException]:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: TLS connection failed — Chain validation failed`
 * - Any other [IOException]:
 *   `POST video.stream-io-api.com/video/call/default/call-123/join?connection_id=connection-456 failed: network I/O failed — unexpected end of stream`
 *
 * The `original-message` is supplied by OkHttp or the JVM through [IOException.message], not by the
 * server. It is omitted when blank. The generic message `timeout` is also omitted because it adds no
 * information to `request timed out`.
 *
 * Resolved path parameters and allowlisted diagnostic query parameters are included to identify the
 * exact request. Every other query parameter is omitted, so newly introduced credentials remain
 * excluded by default. Request headers are never included. The original [IOException] is retained as
 * the cause.
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
    val sanitizedUrl = url.newBuilder()
        .query(null)
        .apply {
            repeat(url.querySize) { index ->
                val name = url.queryParameterName(index)
                if (name in CoordinatorRequestErrorMessage.ALLOWED_QUERY_PARAMETER_NAMES) {
                    addQueryParameter(name, url.queryParameterValue(index))
                }
            }
        }
        .build()
    val query = sanitizedUrl.encodedQuery?.let { "?$it" }.orEmpty()
    return "$method ${sanitizedUrl.host}${sanitizedUrl.encodedPath}$query"
}

private fun IOException.failureDescription(): String {
    val category = failureCategory()
    val detail = usefulFailureDetail(category)
    return if (detail == null) {
        category
    } else {
        "$category${CoordinatorRequestErrorMessage.CAUSE_SEPARATOR}$detail"
    }
}

private fun IOException.failureCategory(): String =
    when (this) {
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

private fun IOException.usefulFailureDetail(category: String): String? {
    val originalMessage = message
    if (originalMessage.isNullOrBlank()) {
        return null
    }

    val isGenericTimeout = category == CoordinatorRequestErrorMessage.REQUEST_TIMED_OUT &&
        originalMessage.equals(CoordinatorRequestErrorMessage.TIMEOUT_KEYWORD, ignoreCase = true)
    return if (isGenericTimeout) null else originalMessage
}
