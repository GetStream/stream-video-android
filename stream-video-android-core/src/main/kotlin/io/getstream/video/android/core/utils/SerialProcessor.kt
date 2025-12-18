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

import io.getstream.log.taggedLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A processor that executes submitted jobs in a serial fashion.
 */
@Suppress("UNCHECKED_CAST")
internal class SerialProcessor(
    private val scope: CoroutineScope,
) {
    private val channel = Channel<JobItem>(Channel.UNLIMITED)
    private var workerJob: Job? = null
    private val logger by taggedLogger("SerialProcessor")
    private var jobCounter = 0
    private var completedJobs = 0
    private var queuedJobs = 0

    private data class JobItem(
        val block: suspend () -> Any?,
        val reply: CompletableDeferred<Result<Any?>>,
        val jobId: Int,
        val jobName: String,
    )

    /**
     * Submit a job to the processor.
     * If the processor is not running, it will be started automatically.
     *
     * @param jobName A descriptive name for the job for logging purposes.
     * @param handler The job to be executed.
     * @return The result of the job.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun <T : Any?> submit(
        jobName: String,
        handler: suspend () -> T,
    ): Result<T> {
        val currentJobId = ++jobCounter
        logger.d {
            "[submit] Job #$currentJobId '$jobName' submitted to SerialProcessor (worker running: ${workerJob?.isActive == true})"
        }

        // Start the worker job if it's not already running
        if (workerJob == null) {
            logger.d { "[submit] Starting SerialProcessor worker" }
            workerJob = scope.launch {
                try {
                    logger.d { "[execute] SerialProcessor worker started" }
                    for (job in channel) {
                        logger.d { "[execute] Job #${job.jobId} '${job.jobName}' starting execution" }
                        val startTime = System.currentTimeMillis()

                        // run the block, capture success or failure
                        val result = runCatching { job.block() }

                        val executionTime = System.currentTimeMillis() - startTime
                        completedJobs++
                        queuedJobs--
                        if (result.isSuccess) {
                            logger.d {
                                "[execute] Job #${job.jobId} '${job.jobName}' completed execution in ${executionTime}ms (completed: $completedJobs, queued: $queuedJobs)"
                            }
                        } else {
                            logger.w {
                                "[execute] Job #${job.jobId} '${job.jobName}' failed after ${executionTime}ms: ${result.exceptionOrNull()?.message} (completed: $completedJobs, queued: $queuedJobs)"
                            }
                        }

                        job.reply.complete(result)
                    }
                    logger.d { "[execute] SerialProcessor worker finished processing all jobs in channel" }
                } catch (e: Exception) {
                    logger.e(e) { "[execute] SerialProcessor (${jobName}) worker crashed: ${e.message}" }
                } finally {
                    logger.d { "[execute] SerialProcessor worker ended (isActive: ${workerJob?.isActive})" }
                    workerJob = null
                }
            }
        }

        val reply = CompletableDeferred<Result<Any?>>()
        if (!channel.isClosedForSend) {
            channel.send(
                JobItem(
                    block = handler,
                    reply = reply,
                    jobId = currentJobId,
                    jobName = jobName,
                ),
            )
            queuedJobs++
            logger.d { "[submit] Job #$currentJobId '$jobName' queued successfully (queued: $queuedJobs)" }
        } else {
            logger.e { "[submit] Job #$currentJobId '$jobName' failed - channel is closed" }
            return Result.failure(Exception("[SerialProcessor] Channel is closed"))
        }

        val finalResult = reply.await().fold(
            onSuccess = { Result.success(it as T) },
            onFailure = { Result.failure(it) },
        )

        if (finalResult.isSuccess) {
            logger.d { "[submit] Job #$currentJobId '$jobName' returned successfully" }
        } else {
            logger.w {
                "[submit] Job #$currentJobId '$jobName' returned with failure: ${finalResult.exceptionOrNull()?.message}"
            }
        }

        return finalResult
    }

    /**
     * Get statistics about the SerialProcessor.
     */
    fun getStats(): String {
        val isRunning = workerJob?.isActive == true
        return "SerialProcessor stats: submitted=$jobCounter, completed=$completedJobs, queued=$queuedJobs, running=$isRunning"
    }

    /**
     * Stop the processor.
     */
    fun stop() = safeCall {
        logger.d { "[stop] Stopping SerialProcessor - ${getStats()}" }
        workerJob?.cancel()
        workerJob = null
        logger.d { "[stop] SerialProcessor stopped" }
    }
}
