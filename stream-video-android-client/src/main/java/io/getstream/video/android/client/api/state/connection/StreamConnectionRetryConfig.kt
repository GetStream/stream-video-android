package io.getstream.video.android.client.api.state.connection

/**
 * Configuration for the connection retry.
 *
 * @param maxRetries The maximum number of retries.
 * @param retryInterval The retry interval in milliseconds.
 */
public data class StreamConnectionRetryConfig(
    val maxRetries: Int = 3,
    val retryInterval: Long = 250L,
)