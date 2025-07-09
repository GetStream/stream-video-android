package io.getstream.video.android.client.internal.config

import io.getstream.video.android.client.internal.generated.IntegrationAppConfig

/**
 * Configuration for the Stream socket.
 *
 * @param url The URL to connect to.
 * @param config The integration app config.
 */
internal data class StreamSocketConfig(
    val url: String,
    val config: IntegrationAppConfig? = null
)