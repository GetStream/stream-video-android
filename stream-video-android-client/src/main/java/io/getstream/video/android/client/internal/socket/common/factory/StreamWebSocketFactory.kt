package io.getstream.video.android.client.internal.socket.common.factory

import io.getstream.log.taggedLogger
import io.getstream.video.android.client.internal.generated.IntegrationAppConfig
import io.getstream.video.android.client.internal.common.RequestTrackingHeadersProvider
import io.getstream.video.android.client.internal.config.StreamSocketConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal class StreamWebSocketFactory(
    private val url: String,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val provider: RequestTrackingHeadersProvider
) {
    private val logger by taggedLogger("Video:StreamWebSocketFactory")

    fun createSocket(config: StreamSocketConfig, listener: WebSocketListener): Result<WebSocket> = runCatching {
        logger.v { "[createSocket] url: $url, config: $config" }
        val request = buildRequest(config.url, config.config)
        okHttpClient.newWebSocket(request, listener)
    }

    /**
     * Builds a request with the given endpoint and config.
     *
     * @param context The context.
     * @param endpoint The endpoint.
     * @param config The headers config.
     */
    private fun buildRequest(
        endpoint: String,
        config: IntegrationAppConfig?
    ): Request =
        Request.Builder()
            .url(endpoint)
            .addHeader("Connection", "Upgrade")
            .addHeader("Upgrade", "websocket")
            .addHeader("X-Stream-Client", provider.provideTrackingHeaders(config))
            .build()
}