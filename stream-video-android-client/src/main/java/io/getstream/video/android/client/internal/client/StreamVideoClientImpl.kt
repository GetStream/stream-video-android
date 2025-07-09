package io.getstream.video.android.client.internal.client

import io.getstream.android.push.PushDevice
import io.getstream.android.push.PushProvider
import io.getstream.android.video.generated.models.CreateDeviceRequest
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.kotlin.base.annotation.marker.StreamDsl
import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.StreamCall
import io.getstream.video.android.client.api.StreamVideoClient
import io.getstream.video.android.client.api.listeners.StreamVideoClientListener
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.internal.client.state.MutableStreamVideoClientState
import io.getstream.video.android.client.api.state.StreamVideoClientState
import io.getstream.video.android.client.api.state.connection.StreamConnectionRetryConfig
import io.getstream.video.android.client.api.state.connection.StreamVideoClientConnectionState
import io.getstream.video.android.client.internal.client.state.StreamClientStateImpl
import io.getstream.video.android.client.internal.common.StreamSubscriptionManager
import io.getstream.video.android.client.internal.generated.apis.ProductVideoApi
import io.getstream.video.android.client.internal.log.provideLogger
import io.getstream.video.android.client.internal.socket.coordinator.ConnectionState
import io.getstream.video.android.client.internal.socket.coordinator.CoordinatorSocket
import io.getstream.video.android.client.internal.socket.coordinator.StreamCoordinatorSocketListener
import io.getstream.video.android.client.model.ConnectUserData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
import kotlin.math.min

/**
 * These constants guard against crazy configurations.
 */
internal object RetryConfigGuardDefaults {
    /**
     * Maximum number of retries for the connection.
     */
    const val MAX_RETRIES = 15

    /**
     * Default retry interval in milliseconds.
     */
    const val RETRY_INTERVAL = 10_000L
}

internal class StreamVideoClientImpl(
    private val logger: TaggedLogger = provideLogger(tag = "ClientV2"),
    private val instanceId: String,
    private val clientScope: CoroutineScope = CoroutineScope(Dispatchers.Default) + SupervisorJob() + CoroutineExceptionHandler { coroutineContext, throwable ->
        logger.e(throwable) { "[clientScope] Uncaught exception in coroutine $coroutineContext: $throwable" }
    },
    private val coordinatorSocket: CoordinatorSocket,
    private val subscriptionManager: StreamSubscriptionManager<StreamVideoClientListener>,
    private val productVideoApi: ProductVideoApi,
    private val internalState: MutableStreamVideoClientState = StreamClientStateImpl(),
) : StreamVideoClient {

    private var socketSubscription: StreamSubscription? = null
    private val socketListener = object : StreamCoordinatorSocketListener {

        override fun onState(state: ConnectionState) {
            logger.d { "[onState] Socket state changed: $state" }

            when (state) {
                is ConnectionState.Connected -> {
                    internalState.setConnectionState(
                        StreamVideoClientConnectionState.Connected(
                            state.user, state.connectionId
                        )
                    )
                }

                is ConnectionState.Disconnected -> {
                    internalState.setConnectionState(
                        StreamVideoClientConnectionState.Disconnected(
                            state.error
                        )
                    )
                }
            }

            subscriptionManager.forEach {
                it.onState(internalState)
            }.onFailure {
                logger.e(it) { "[onState] Failed to notify listeners of state change: $state" }
            }
        }

        override fun onEvent(event: VideoEvent) {
            subscriptionManager.forEach { it.onEvent(event) }.onFailure {
                logger.e(it) { "[onEvent] Failed to notify listeners of event: $event" }
            }
        }

    }

    // region: API
    override fun subscribe(listener: StreamVideoClientListener): Result<StreamSubscription> =
        subscriptionManager.subscribe(listener)

    override fun call(type: String, id: String): Result<StreamCall> = runCatching {
        TODO()
    }

    override suspend fun connect(
        data: ConnectUserData,
        retry: StreamConnectionRetryConfig,
    ): StreamVideoClientConnectionState = connectAttempt(0, data, retry)


    override suspend fun registerDevice(pushDevice: PushDevice): Result<PushDevice> = runCatching {
        logger.d { "[registerDevice] Registering device: $pushDevice" }
        val pushProvider = when (pushDevice.pushProvider) {
            PushProvider.FIREBASE -> CreateDeviceRequest.PushProvider.Firebase
            PushProvider.HUAWEI -> CreateDeviceRequest.PushProvider.Huawei
            PushProvider.XIAOMI -> CreateDeviceRequest.PushProvider.Xiaomi
            PushProvider.UNKNOWN -> throw IllegalArgumentException("Unsupported PushProvider")
        }

        val request = CreateDeviceRequest(
            id = pushDevice.token,
            pushProvider = pushProvider,
            pushProviderName = pushDevice.providerName,
        )

        productVideoApi.createDevice(request)
        pushDevice
    }

    override suspend fun unregisterDevice(token: String): Result<Unit> = runCatching {
        logger.d { "[unregisterDevice] Unregistering device with token: $token" }
        productVideoApi.deleteDevice(token)
    }

    override suspend fun disconnect(): Result<Unit> {
        val disconnectResult = coordinatorSocket.disconnect()
        return disconnectResult
    }

    override fun getState(): Result<StreamVideoClientState> = runCatching {
        internalState
    }


    // region end: API

    // region internal

    private suspend fun connectAttempt(
        attempts: Int = 0, data: ConnectUserData, retry: StreamConnectionRetryConfig
    ): StreamVideoClientConnectionState = catchingState {

        val guardedConfig = guardRetryConfig(retry)

        val current = internalState.getConnectionState()
        logger.v { "[connect] Current state: $current" }

        current.onConnected {
            logger.w { "[connect] Client is already connected!" }
        }

        current.onDisconnected {
            logger.v { "[connect] Connecting client with data: $data" }

            socketSubscription = coordinatorSocket.subscribe(socketListener).getOrThrow()

            coordinatorSocket.connect(data).mapRight { connected ->
                logger.d { "[connect] Connected client with data: $data" }
                val connectionState = StreamVideoClientConnectionState.Connected(
                    connected.user, connected.connectionId
                )
                internalState.setConnectionState(connectionState)
                subscriptionManager.forEach {
                    it.onState(internalState)
                }
            }.mapLeft { disconnected ->
                if (disconnected.apiError != null) {
                    // API Error
                    val unrecoverable = disconnected.apiError.unrecoverable ?: true
                    if (unrecoverable) {
                        val connectionState =
                            StreamVideoClientConnectionState.Disconnected(apiError = disconnected.apiError)
                        internalState.setConnectionState(connectionState)
                        subscriptionManager.forEach {
                            it.onState(internalState)
                        }
                    } else {
                        if (attempts < guardedConfig.maxRetries) {
                            logger.d { "[connect] Retrying connection. Attempt: $attempts" }
                            delay(guardedConfig.retryInterval)
                            connectAttempt(attempts + 1, data, retry)
                        } else {
                            logger.d { "[connect] Failed to connect after $attempts attempts" }
                            val connectionState =
                                StreamVideoClientConnectionState.Disconnected(apiError = disconnected.apiError)
                            internalState.setConnectionState(connectionState)
                            subscriptionManager.forEach {
                                it.onState(internalState)
                            }
                        }
                    }
                } else {
                    logger.e(
                        it.error ?: Exception("Failed to connect client (unknown)")
                    ) { "[connect] Failed to connect client. ${it.error?.message}" }
                    val connectionState =
                        StreamVideoClientConnectionState.Disconnected(disconnected.error)
                    internalState.setConnectionState(connectionState)
                    subscriptionManager.forEach {
                        it.onState(internalState)
                    }
                }
            }
        }
    }

    private fun guardRetryConfig(retry: StreamConnectionRetryConfig): StreamConnectionRetryConfig {
        if (retry.maxRetries > RetryConfigGuardDefaults.MAX_RETRIES) {
            logger.w {
                "[guardRetryConfig] Max retries is too high. Limiting to ${RetryConfigGuardDefaults.MAX_RETRIES}"
            }
        }

        if (retry.retryInterval > RetryConfigGuardDefaults.RETRY_INTERVAL) {
            logger.w {
                "[guardRetryConfig] Retry interval is too high. Limiting to ${RetryConfigGuardDefaults.RETRY_INTERVAL}"
            }
        }
        return retry.copy(
            maxRetries = min(retry.maxRetries, RetryConfigGuardDefaults.MAX_RETRIES),
            retryInterval = min(retry.retryInterval, RetryConfigGuardDefaults.RETRY_INTERVAL),
        )
    }

    @StreamDsl
    private inline fun catchingState(block: () -> StreamVideoClientConnectionState): StreamVideoClientConnectionState {
        return try {
            block()
        } catch (e: Exception) {
            StreamVideoClientConnectionState.Disconnected(e)
        }
    }
}