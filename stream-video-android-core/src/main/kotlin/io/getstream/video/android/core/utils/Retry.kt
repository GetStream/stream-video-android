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

package io.getstream.video.android.core.utils

import io.getstream.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

internal class QueuedJobScope {
    lateinit var updateBlock: (Job) -> Unit
    lateinit var execute: suspend () -> Unit

    fun update(block: (Job) -> Unit) {
        this.updateBlock = block
    }

    fun execute(block: suspend () -> Unit) {
        execute = block
    }
}

internal inline fun waitForJob(scope: CoroutineScope, job: Job? = null, timeout: Long = 1500L, block: QueuedJobScope.() -> Unit) {
    val jobScope = QueuedJobScope()
    jobScope.block()
    val new = scope.launch {
        try {
            withTimeout(timeout) {
                job?.join()
            }
        } catch (e: CancellationException) {
            job?.cancel()
        }
        jobScope.execute.invoke()
    }
    jobScope.updateBlock.invoke(new)
}

/**
 * Retries the given operation based on the retry spec.
 */
internal suspend fun <T : Any> retry(
    retries: Int,
    initialDelay: Long = 100,
    delayCalculation: (Int, Long) -> Long = { attempt, prevDelay -> attempt * prevDelay },
    operation: suspend () -> T,
): Result<T> = safeSuspendingCallWithResult {
    var currentRetry = 1
    var delayMillis = initialDelay
    repeat(retries) {
        try {
            return@safeSuspendingCallWithResult operation()
        } catch (e: Exception) {
            if (currentRetry >= retries) {
                throw e
            }
            delayMillis = delayCalculation(currentRetry, delayMillis)
            delay(delayMillis)
            currentRetry++
        }
    }
    throw IllegalStateException("Retry logic failed unexpectedly")
}
