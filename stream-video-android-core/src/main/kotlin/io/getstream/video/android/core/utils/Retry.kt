package io.getstream.video.android.core.utils

import io.getstream.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

internal class QueuedJobScope
{
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
    operation: suspend () -> T
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