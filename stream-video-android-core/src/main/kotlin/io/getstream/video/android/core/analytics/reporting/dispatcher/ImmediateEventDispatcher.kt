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

package io.getstream.video.android.core.analytics.reporting.dispatcher

import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.android.video.generated.models.ReportClientEventRequest
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.analytics.reporting.datasource.InMemoryPendingEventDataSource
import io.getstream.video.android.core.analytics.reporting.datasource.PendingEventDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.getValue

/**
 * Sends each event immediately in a coroutine.
 * On network failure the events are saved to [dataSource] for later retry.
 */
internal class ImmediateEventDispatcher(
    private val api: ProductvideoApi,
    private val scope: CoroutineScope,
    private val dataSource: PendingEventDataSource = InMemoryPendingEventDataSource(),
) : EventDispatcher {

    private companion object {
        const val RETRY_MAX_ATTEMPT = 5
        const val BASE_RETRY_DELAY_MS = 500L
    }

    private val logger by taggedLogger("ImmediateEventSender")

    override fun send(event: ClientEvent) = sendAll(listOf(event))

    override fun sendAll(events: List<ClientEvent>) {
        if (events.isEmpty()) return
        scope.launch {
            retryWithBackoff {
                logger.d { events.joinToString(",") { it.toLog() } }
                api.reportClientCallEvent(ReportClientEventRequest(events))
            }.onFailure { e ->
                logger.w { "[sendAll] Failed — sending ${events.size} event(s) for retry: ${e.message}" }
                logger.w { events.joinToString(",") { it.toLog() } }
                if (e !is NonRetryableException) {
                    dataSource.save(events)
                }
            }
        }
    }

    override fun retryPending() {
        val pending = dataSource.loadAndClear()
        if (pending.isEmpty()) return
        sendAll(pending)
    }

    override fun deleteAll() {
        scope.cancel()
        dataSource.clear()
    }

    suspend fun retryWithBackoff(maxAttempts: Int = RETRY_MAX_ATTEMPT, lambda: suspend () -> Unit): Result<Unit> {
        val baseDelayMs = BASE_RETRY_DELAY_MS

        repeat(maxAttempts) { attempt ->

            val result = runCatching { lambda() }

            if (result.isSuccess) {
                return result
            }

            val exception = result.exceptionOrNull()

            if (!shouldRetry(exception)) {
                return Result.failure(NonRetryableException(exception))
            }

            if (attempt < maxAttempts - 1) {
                val delayMs = baseDelayMs * (1L shl attempt) // 500, 1000, 2000, 4000, 8000
                delay(delayMs)
            }
        }

        return Result.failure(RetryExhaustedException(null))
    }

    fun shouldRetry(throwable: Throwable?): Boolean {
        return when (throwable) {
            is HttpException -> {
                throwable.code() in 500..599
            }

            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is IOException,
            -> {
                true
            }

            else -> false
        }
    }
}

private sealed class RetryException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

private class NonRetryableException(
    cause: Throwable?,
) : RetryException(
    message = "Failure is not retryable",
    cause = cause,
)

private class RetryExhaustedException(
    cause: Throwable?,
) : RetryException(
    message = "Retry attempts exhausted",
    cause = cause,
)
