package io.getstream.video.android.client.internal.config

import io.getstream.kotlin.base.annotation.marker.StreamConfiguration
import io.getstream.video.android.client.internal.generated.IntegrationAppConfig
import io.getstream.video.android.client.internal.socket.coordinator.StreamCoordinatorConfig
import io.getstream.video.android.client.model.ConnectUserData

/**
 * Configuration for the Stream client.
 *
 * @param apiKey The API key for the Stream client.
 * @param baseUrl The base URL for the Stream client.
 * @param integrationAppConfig The integration app config for the Stream client.
 * @param socketConfig The socket config for the Stream client.
 * @param coordinatorConfig The coordinator config for the Stream client.
 * @param instanceConfig The instance config for the Stream client.
 */
internal data class StreamClientConfig(
    val apiKey: String,
    val baseUrl: String,
    val integrationAppConfig: IntegrationAppConfig,
    val socketConfig: StreamSocketConfig = StreamSocketConfig(
        baseUrl,
        integrationAppConfig,
    ),
    val coordinatorConfig: StreamCoordinatorConfig = StreamCoordinatorConfig(
        apiKey,
        socketConfig,
    ),
    val instanceConfig: StreamInstanceConfig = StreamInstanceConfig()
)