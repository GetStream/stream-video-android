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
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLKeyException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException

private object CoordinatorRequestErrorMessage {
    const val DNS_LOOKUP_FAILED: String = "DNS lookup failed"
    const val REQUEST_TIMED_OUT: String = "request timed out"
    const val REQUEST_INTERRUPTED: String = "request interrupted"
    const val CONNECTION_FAILED: String = "connection failed"
    const val TLS_CONNECTION_FAILED: String = "TLS connection failed"
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
 * The response code and payload are not changed, so Retrofit error parsing and authentication
 * retries continue to work normally. The response body source is decorated so recognized transport
 * exceptions raised while consuming the payload receive the same endpoint context as those raised
 * by [Interceptor.Chain.proceed].
 *
 * Recognized transport failures are recreated using their existing exception category and a message
 * in this format:
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
 *
 * The `original-message` is supplied by OkHttp or the JVM through [IOException.message], not by the
 * server. It is omitted when blank. The generic message `timeout` is also omitted because it adds no
 * information to `request timed out`.
 *
 * Resolved path parameters and allowlisted diagnostic query parameters are included to identify the
 * exact request. Every other query parameter is omitted, so newly introduced credentials remain
 * excluded by default. Request headers are never included. A recreated exception copies the
 * original stack trace, direct cause, suppressed exceptions, and transferred-byte count where
 * applicable, so formatting the message does not add another level to the exception hierarchy.
 * Unrecognized [IOException] types are rethrown unchanged.
 */
internal class CoordinatorRequestErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            val response = chain.proceed(request).withRequestContextOnBodyReadFailure(request)
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
            throw exception.recreateWithFormattedMessage(request)
        }
    }
}

private fun Response.withRequestContextOnBodyReadFailure(request: Request): Response {
    val responseBody = body ?: return this
    val source = object : ForwardingSource(responseBody.source()) {
        override fun read(sink: Buffer, byteCount: Long): Long =
            try {
                super.read(sink, byteCount)
            } catch (exception: IOException) {
                throw exception.recreateWithFormattedMessage(request)
            }
    }.buffer()

    return newBuilder()
        .body(source.asResponseBody(responseBody.contentType(), responseBody.contentLength()))
        .build()
}

/**
 * Restores coordinator request context when OkHttp's call timeout replaces a recognized exception
 * thrown by the interceptor with a new [InterruptedIOException].
 */
internal fun IOException.restoreCoordinatorRequestContextFromCause(): IOException {
    if (coordinatorRequestDescription() != null) return this

    var nestedCause = cause
    while (nestedCause != null && nestedCause !== nestedCause.cause) {
        if (nestedCause is IOException) {
            val requestDescription = nestedCause.coordinatorRequestDescription()
            if (requestDescription != null) {
                return recreateWithFormattedDescription(requestDescription)
            }
        }
        nestedCause = nestedCause.cause
    }
    return this
}

private fun IOException.recreateWithFormattedMessage(request: Request): IOException =
    recreateWithFormattedDescription(request.sanitizedDescription())

private fun IOException.recreateWithFormattedDescription(
    requestDescription: String,
): IOException {
    val failureDescription = failureDescription() ?: return this
    val contextualMessage =
        requestDescription + CoordinatorRequestErrorMessage.FAILURE_SEPARATOR + failureDescription
    val contextualException: IOException = when (javaClass) {
        UnknownHostException::class.java -> UnknownHostException(contextualMessage)
        SocketTimeoutException::class.java -> SocketTimeoutException(contextualMessage)
        InterruptedIOException::class.java -> InterruptedIOException(contextualMessage)
        ConnectException::class.java -> ConnectException(contextualMessage)
        SSLHandshakeException::class.java -> SSLHandshakeException(contextualMessage)
        SSLKeyException::class.java -> SSLKeyException(contextualMessage)
        SSLPeerUnverifiedException::class.java -> SSLPeerUnverifiedException(contextualMessage)
        SSLProtocolException::class.java -> SSLProtocolException(contextualMessage)
        SSLException::class.java -> SSLException(contextualMessage)
        else -> return this
    }
    return contextualException.copyDiagnosticStateFrom(this)
}

private fun IOException.copyDiagnosticStateFrom(original: IOException): IOException = apply {
    stackTrace = original.stackTrace
    initCause(original.cause)
    original.suppressed.forEach(::addSuppressed)

    if (this is InterruptedIOException && original is InterruptedIOException) {
        bytesTransferred = original.bytesTransferred
    }
}

private fun IOException.coordinatorRequestDescription(): String? {
    val message = message ?: return null
    val separatorIndex = message.indexOf(CoordinatorRequestErrorMessage.FAILURE_SEPARATOR)
    return if (separatorIndex > 0) message.substring(0, separatorIndex) else null
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

private fun IOException.failureDescription(): String? {
    val category = failureCategory() ?: return null
    val detail = usefulFailureDetail(category)
    return if (detail == null) {
        category
    } else {
        "$category${CoordinatorRequestErrorMessage.CAUSE_SEPARATOR}$detail"
    }
}

private fun IOException.failureCategory(): String? =
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
        else -> null
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
