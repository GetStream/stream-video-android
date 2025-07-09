package io.getstream.video.android.client.api.builder

import android.content.Context
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.kotlin.base.annotation.marker.StreamDelicateApi
import io.getstream.kotlin.base.annotation.marker.StreamFactory
import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.StreamVideoClient
import io.getstream.video.android.client.internal.client.StreamVideoClientImpl
import io.getstream.video.android.client.internal.client.StreamVideoClientRegistry
import io.getstream.video.android.client.internal.common.RequestTrackingHeadersProvider
import io.getstream.video.android.client.internal.common.StreamSubscriptionManagerImpl
import io.getstream.video.android.client.internal.config.SameIdInstanceBehaviour
import io.getstream.video.android.client.internal.config.StreamClientConfig
import io.getstream.video.android.client.internal.socket.coordinator.StreamCoordinatorConfig
import io.getstream.video.android.client.internal.config.StreamInstanceConfig
import io.getstream.video.android.client.internal.config.StreamSocketConfig
import io.getstream.video.android.client.internal.generated.IntegrationAppConfig
import io.getstream.video.android.client.internal.generated.apis.ProductVideoApi
import io.getstream.video.android.client.internal.log.provideLogger
import io.getstream.video.android.client.internal.processing.DebounceProcessor
import io.getstream.video.android.client.internal.serialization.CoordinatorParser
import io.getstream.video.android.client.internal.socket.common.StreamWebSocketImpl
import io.getstream.video.android.client.internal.socket.common.factory.StreamWebSocketFactory
import io.getstream.video.android.client.internal.socket.coordinator.CoordinatorAuthInterceptor
import io.getstream.video.android.client.internal.socket.coordinator.CoordinatorSocket
import io.getstream.video.android.client.internal.monitor.StreamHealthMonitor
import io.getstream.video.android.client.model.ConnectUserData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Scope for the [streamVideoClient] builder function.
 */
public class StreamVideoClientBaseScope {
    public var apiKey: String? = null
    public var userId: String? = null
    public var token: String? = null
    public var appName: String? = null
    public var appVersion: String? = null

    @StreamDelicateApi
    public var baseUrl: String = "https://video.stream-io-api.com"

    @StreamDelicateApi
    public var wssUrl: String = "wss://video.stream-io-api.com/video/connect"

    @OptIn(StreamDelicateApi::class)
    internal fun build(context: Context): StreamClientConfig {
        val integrationAppConfig = IntegrationAppConfig(
            appVersion = appVersion ?: context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionName,
            appName = appName ?: context.getString(context.applicationInfo.labelRes),
        )
        val socketConfig = StreamSocketConfig(
            url = wssUrl,
            config = integrationAppConfig,
        )
        return StreamClientConfig(
            apiKey = apiKey ?: throw IllegalArgumentException("API key is required"),
            baseUrl = baseUrl,
            integrationAppConfig = integrationAppConfig,
            socketConfig = socketConfig,
            coordinatorConfig = StreamCoordinatorConfig(
                apiKey = apiKey ?: throw IllegalArgumentException("API key is required"),
                socketConfig = socketConfig,
                connectData = ConnectUserData(
                    token = token ?: throw IllegalArgumentException("Token is required"),
                    id = userId ?: throw IllegalArgumentException("User ID is required"),
                ),
            ),
            instanceConfig = StreamInstanceConfig()
        )
    }
}

/**
 * Builder for creating a [StreamVideoClient] instance.
 *
 * @param context The Android context.
 * @param lambda The lambda to configure the builder.
 * @return The created [StreamVideoClient] instance.
 */
@StreamFactory
public fun streamVideoClient(
    id: String,
    context: Context,
    coroutineScope: CoroutineScope? = null,
    lambda: StreamVideoClientBaseScope.() -> Unit
): Result<StreamVideoClient> = runCatching {

    val logger: TaggedLogger = Log.logger
    val scope = StreamVideoClientBaseScope()
    scope.lambda()

    val config = scope.build(context)

    val previousInstance = StreamVideoClientRegistry.get(id)
    if (previousInstance != null) {
        when (config.instanceConfig.sameIdInstanceBehaviour) {
            SameIdInstanceBehaviour.REPLACE -> {
                logger.w {
                    """
                    |Replacing existing StreamVideoClient with id `$id`.
                    |— The old client (and its socket/health‐monitor) will be torn down.
                    |— Make sure you’ve cleaned up any listeners or subscriptions attached to the previous client.
                    """.trimMargin()
                }
            }

            SameIdInstanceBehaviour.RETURN_EXISTING -> {
                logger.w {
                    """
                    |StreamVideoClient with id `$id` already exists — returning the existing instance.
                    |— Your new `instanceConfig` was ignored.
                    |— If you need to change configs at runtime, call `existingClient.updateConfig(...)` instead of re-creating.
                    """.trimMargin()
                }
                return@runCatching previousInstance
            }

            SameIdInstanceBehaviour.ERROR -> {
                throw IllegalStateException(
                    """ 
                    |A StreamVideoClient with id `$id` already exists. 
                    |— If you meant to replace it, set sameIdInstanceBehaviour=REPLACE; 
                    |— if you meant to reuse it, set sameIdInstanceBehaviour=RETURN_EXISTING.
                    """.trimMargin()
                )
            }
        }
    }


    val clientsCount = StreamVideoClientRegistry.size()
    if (clientsCount > 5) {
        logger.w { "You have created $clientsCount clients. Please make sure this is intentional!" }
    }

    if (clientsCount >= config.instanceConfig.numOfInstances) {
        throw IllegalStateException("Max number of clients reached. You are not allowed to create any more clients.")
    }

    // Build
    val internalScope = (coroutineScope
        ?: CoroutineScope(Dispatchers.Default)) + SupervisorJob() + CoroutineExceptionHandler { coroutineContext, throwable ->
        logger.e(throwable) { "[clientScope] Uncaught exception in coroutine $coroutineContext: $throwable" }
    }
    val coordinatorAuthInterceptor = CoordinatorAuthInterceptor(
        apiKey = config.coordinatorConfig.apiKey,
    ) {
        config.coordinatorConfig.connectData?.token
            ?: throw IllegalArgumentException("Token is required")
    }

    val okHttpClient = OkHttpClient.Builder().addInterceptor(
        coordinatorAuthInterceptor
    ).build()

    val coordinatorSocket = CoordinatorSocket(
        config = config.coordinatorConfig,
        internalSocket = StreamWebSocketImpl(
            socketFactory = StreamWebSocketFactory(
                url = config.coordinatorConfig.socketConfig.url,
                okHttpClient = okHttpClient,
                provider = RequestTrackingHeadersProvider(),
            ), subscriptionManager = StreamSubscriptionManagerImpl()
        ),
        coordinatorParser = CoordinatorParser(Serializer.moshi),
        healthMonitor = StreamHealthMonitor(scope = internalScope),
        debounceProcessor = DebounceProcessor(
            scope = internalScope,
        ),
        subscriptionManager = StreamSubscriptionManagerImpl()
    )

    val productVideoApi: ProductVideoApi = Retrofit.Builder().baseUrl(config.baseUrl)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi)).client(okHttpClient)
        .build().create(ProductVideoApi::class.java)

    val client = StreamVideoClientImpl(
        logger = provideLogger(tag = "$id:Client"),
        instanceId = id,
        clientScope = internalScope,
        coordinatorSocket = coordinatorSocket,
        subscriptionManager = StreamSubscriptionManagerImpl(),
        productVideoApi = productVideoApi,
    )

    StreamVideoClientRegistry.put(id, client)
    client
}

// Internal logic
private object Log {
    val logger: TaggedLogger = provideLogger(tag = "Client")
}