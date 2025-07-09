package io.getstream.video.android.client.internal.client

import io.getstream.video.android.client.api.StreamVideoClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the instances of the [StreamVideoClient] to be used by the [StreamVideo] client.
 */
internal object StreamVideoClientRegistry {

    private val instances: ConcurrentHashMap<String, StreamVideoClient> = ConcurrentHashMap(1)

    /**
     * Preserves the instance of the [StreamVideoClient] to be used.
     *
     * @param id The id of the instance.
     * @param instance The instance to be preserved.
     */
    public fun put(id: String, instance: StreamVideoClient) {
        instances[id] = instance
    }

    /**
     * Removes the instance of the [StreamVideoClient] to be used.
     *
     * @param id The id of the instance.
     */
    public fun remove(id: String) {
        instances.remove(id)
    }

    /**
     * Returns the instance of the [StreamVideoClient] to be used.
     *
     * @param id The id of the instance.
     * @return The instance of the [StreamVideoClient] or null if it doesn't exist.
     */
    public fun get(id: String): StreamVideoClient? {
        return instances[id]
    }

    fun size() = instances.size
}