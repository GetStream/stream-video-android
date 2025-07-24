package io.getstream.video.android.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

/**
 * A processor that executes submitted jobs in a serial fashion.
 */
@Suppress("UNCHECKED_CAST")
internal class SerialProcessor(
    private val scope: CoroutineScope
) {
    private val channel = Channel<JobItem>(Channel.UNLIMITED)
    private var workerJob: Job? = null

    private data class JobItem(
        val block: suspend () -> Any?,
        val reply: CompletableDeferred<Result<Any?>>
    )

    /**
     * Submit a job to the processor.
     * If the processor is not running, it will be started automatically.
     *
     * @param handler The job to be executed.
     * @return The result of the job.
     */
    suspend fun <T : Any> submit(
        handler: suspend () -> T
    ): Result<T> {
        // Start the worker job if it's not already running
        if (workerJob == null) {
            workerJob = scope.launch {
                for (job in channel) {
                    // run the block, capture success or failure
                    val result = runCatching { job.block() }
                    job.reply.complete(result)
                }
            }
        }

        val reply = CompletableDeferred<Result<Any?>>()
        channel.send(
            JobItem(
                block = handler,
                reply = reply
            )
        )
        return reply.await().fold(
            onSuccess  = { Result.success(it as T) },
            onFailure  = { Result.failure(it) }
        )
    }

    /**
     * Stop the processor.
     */
    fun stop() {
        workerJob?.cancel()
        workerJob = null
    }
}


