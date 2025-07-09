package io.getstream.video.android.client.internal.processing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Buffers items sent via [onMessage], and whenever either
 * it emits that batch (in arrival order) via your provided [onBatch] handler.
 *
 * @param scope The coroutine scope to use for the debounce processor.
 * @param batchSize The maximum number of items to buffer before emitting a batch.
 * @param initialDelayMs The initial delay in milliseconds before emitting a batch.
 * @param maxDelayMs The maximum delay in milliseconds before emitting a batch.
 */
internal class DebounceProcessor<T>(
    private val scope: CoroutineScope,
    private val batchSize: Int = 10,
    private val initialDelayMs: Long = 100L,
    private val maxDelayMs: Long = 1_000L,
) {
    private var batchHandler: suspend (List<T>, Long, Int) -> Unit = { _, _, _ -> }
    private var actor: SendChannel<T>? = null

    /**
     * Starts the debounce processor.
     */
    @OptIn(ObsoleteCoroutinesApi::class)
    fun start() = runCatching {
        actor = scope.actor(capacity = Channel.UNLIMITED) {
            batchingLoop()
        }
    }

    /**
     * Stops the debounce processor.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun stop() = runCatching {
        if (actor?.isClosedForSend == false) {
            actor?.close()
        }
    }

    /**
     * Registers a handler for batches of items.
     * First lambda parameter is the batch of items,
     * second lambda parameter is the debounce time in milliseconds that was applied for this batch,
     * third lambda parameter is the total size of the buffered events.
     *
     * @param handler The handler to be called when a batch is ready.
     */
    fun onBatch(handler: suspend (List<T>, Long, Int) -> Unit) {
        batchHandler = handler
    }

    fun onMessage(item: T) {
        if (actor == null) {
            // Start if we receive a message
            start()
        }
        actor?.trySend(item)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private suspend fun ActorScope<T>.batchingLoop() {
        try {
            val buffer = mutableListOf<T>()
            var currentDelay = initialDelayMs

            while (isActive) {
                val first = channel.receive()
                buffer += first
                val batchStart = System.currentTimeMillis()

                collectUntilTimeout(batchStart, currentDelay, buffer)

                val wasFull = buffer.size >= batchSize
                batchHandler(buffer.toList(), currentDelay, buffer.size)
                buffer.clear()

                currentDelay = if (wasFull) backoff(currentDelay) else initialDelayMs
                delay(currentDelay)
            }
        } catch (_: ClosedReceiveChannelException) {
            // Channel is closed for receiving
        }
    }

    /** Double the window, capped at maxDelayMs */
    private fun backoff(delayMs: Long): Long = (delayMs * 2).coerceAtMost(maxDelayMs)

    /** Keep pulling from the channel until size==batchSize or timeout expires */
    @OptIn(ObsoleteCoroutinesApi::class)
    private suspend fun ActorScope<T>.collectUntilTimeout(
        startTime: Long,
        windowMs: Long,
        buffer: MutableList<T>
    ) {
        while (buffer.size < batchSize) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= windowMs) break
            val remaining = windowMs - elapsed
            val next = withTimeoutOrNull(remaining) { channel.receive() }
            if (next != null) buffer += next else break
        }
    }
}
