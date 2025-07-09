package io.getstream.video.android.client.internal.common

import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.subscribe.StreamSubscriber
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.internal.log.provideLogger
import java.util.concurrent.ConcurrentHashMap

internal interface StreamSubscriptionManager<T : StreamSubscriber> {

    /**
     * Subscribes a listener to the manager.
     *
     * @param listener The listener to subscribe.
     * @return The subscription ID.
     */
    fun subscribe(listener: T): Result<StreamSubscription>

    /**
     * Clears all the listeners from the manager.
     *
     * @return The result of the operation.
     */
    fun clear(): Result<Unit>

    /**
     * Iterates over all the listeners.
     *
     * @param block The block to execute for each listener.
     * @return The result of the operation.
     */
    fun forEach(block: (T) -> Unit): Result<Unit>
}

/**
 * Manages the subscribers of any object.
 */
internal class StreamSubscriptionManagerImpl<T : StreamSubscriber>(
    private val logger: TaggedLogger = provideLogger(tag = "SubscriptionManager"),
    private val maxSubscriptions: Int = MAX_LISTENERS
) : StreamSubscriptionManager<T> {
    companion object {
        /**
         * Maximum number of listeners.
         */
        internal const val MAX_LISTENERS = 250
    }

    private val subscribers = ConcurrentHashMap<Int, T>()
    private var nextId = 0

    override fun subscribe(listener: T): Result<StreamSubscription> = runCatching {
        if (nextId > maxSubscriptions) {
            logger.e {
                """
                |[subscribe] Failed, too many subscribers (size: ${subscribers.size}, max: ${maxSubscriptions})
                | - The default MAX_LISTENERS is 250. This limit is set to avoid mistakes and "loop" subscriptions.
                | - If you intentionally need to go over 250 listeners, supply this in the configuration.
                """".trimMargin()
            }
            throw IllegalStateException("Max listeners reached, unsubscribe some listeners.")
        }
        val id = nextId + 1
        subscribers[id] = listener
        logger.v { "[subscribe] Subscribed with id: $id" }
        nextId = id
        object : StreamSubscription {
            override val id: Int = id
            override fun cancel() = unsubscribe(id)
        }
    }

    override fun clear(): Result<Unit> = runCatching {
        subscribers.clear()
        logger.v { "[clear] Cleared all subscribers" }
    }

    override fun forEach(block: (T) -> Unit) = runCatching {
        subscribers.forEach { (_, listener) ->
            block(listener)
        }
    }

    private fun unsubscribe(id: Int): Result<Unit> = runCatching {
        subscribers.remove(id)
        logger.v { "[unsubscribe] Removed subscriber with id: $id" }
    }
}
