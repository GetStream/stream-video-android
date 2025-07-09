package io.getstream.video.android.client.internal.socket.coordinator

import io.getstream.android.video.generated.models.APIError
import io.getstream.android.video.generated.models.ConnectUserDetailsRequest
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.ConnectionErrorEvent
import io.getstream.android.video.generated.models.WSAuthMessageRequest
import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.listeners.StreamVideoEventListener
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.internal.common.algebra.Either
import io.getstream.video.android.client.internal.common.StreamSubscriptionManager
import io.getstream.video.android.client.internal.config.StreamSocketConfig
import io.getstream.video.android.client.internal.log.provideLogger
import io.getstream.video.android.client.internal.processing.DebounceProcessor
import io.getstream.video.android.client.internal.serialization.CoordinatorParser
import io.getstream.video.android.client.internal.socket.common.StreamWebSocket
import io.getstream.video.android.client.internal.socket.common.listeners.StreamWebSocketListener
import io.getstream.video.android.client.internal.monitor.StreamHealthMonitor
import io.getstream.video.android.client.model.ConnectUserData
import io.getstream.video.android.client.model.StreamConnectedUser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

// State
/**
 * Coordinator socket state.
 */
internal sealed class ConnectionState {
    /** Connected state. */
    data class Connected(val user: StreamConnectedUser, val connectionId: String) :
        ConnectionState()

    /**
     * Disconnected state.
     * @param error Optional error that caused the disconnection.
     * @param apiError Optional API error that caused the disconnection.
     */
    data class Disconnected(val error: Throwable? = null, val apiError: APIError? = null) :
        ConnectionState()
}

/**
 * Listener for the coordinator socket state.
 */
internal interface StreamCoordinatorSocketListener : StreamVideoEventListener {
    /**
     * Called when the state changes.
     *
     * @param state The new state.
     */
    fun onState(state: ConnectionState) {}
}

// Config
/**
 * Configuration for the coordinator.
 *
 * @param apiKey The API key to use for the coordinator.
 * @param socketConfig The socket configuration to use for the coordinator.
 * @param connectData The connect data to use for the coordinator.
 */
internal data class StreamCoordinatorConfig(
    val apiKey: String,
    val socketConfig: StreamSocketConfig,
    var connectData: ConnectUserData? = null,
)

// Utils
/**
 * CoordinatorAuthInterceptor adds the token authentication to the API calls
 */
internal class CoordinatorAuthInterceptor(
    var apiKey: String,
    var authType: String = "jwt",
    var token: () -> String,
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val updatedUrl = if (original.url.toString().contains("video")) {
            original.url.newBuilder()
                .addQueryParameter(API_KEY, apiKey)
                .build()
        } else {
            original.url
        }

        val updated = original.newBuilder()
            .url(updatedUrl)
            .addHeader(HEADER_AUTHORIZATION, token.invoke())
            .header(STREAM_AUTH_TYPE, authType)
            .build()

        return chain.proceed(updated)
    }

    private companion object {
        /**
         * Query key used to authenticate to the API.
         */
        private const val API_KEY = "api_key"
        private const val STREAM_AUTH_TYPE = "stream-auth-type"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
}

// Impl
/**
 * Coordinator socket implementation.
 *
 * @param logger The logger to use.
 * @param config The configuration to use.
 * @param internalSocket The internal socket to use.
 * @param coordinatorParser The coordinator parser to use.
 * @param healthMonitor The health monitor to use.
 * @param debounceProcessor The debounce processor to use.
 * @param subscriptionManager The subscription manager to use.
 */
internal class CoordinatorSocket(
    private val logger: TaggedLogger = provideLogger(tag = "CoordinatorSocket"),
    private var config: StreamCoordinatorConfig,
    private val internalSocket: StreamWebSocket<StreamWebSocketListener>,
    private val coordinatorParser: CoordinatorParser,
    private val healthMonitor: StreamHealthMonitor,
    private val debounceProcessor: DebounceProcessor<String>,
    private val subscriptionManager: StreamSubscriptionManager<StreamCoordinatorSocketListener>,
) : StreamSubscriptionManager<StreamCoordinatorSocketListener> by subscriptionManager {

    private var connectionState: ConnectionState = ConnectionState.Disconnected()

    private var messageSubscription: StreamSubscription? = null
    private var connectedEvent: ConnectedEvent? = null
    private val eventListener = object : StreamWebSocketListener {

        override fun onMessage(text: String) {
            logger.v { "[onMessage] Coordinator socket message: $text" }
            debounceProcessor.onMessage(text)
        }

        override fun onFailure(t: Throwable, response: Response?) {
            logger.e(t) { "[onFailure] Coordinator socket failure. ${t.message}" }
            cleanup(t)
        }

        override fun onClosed(code: Int, reason: String) {
            val error = if (code == StreamWebSocket.CLOSE_SOCKET_CODE) {
                null
            } else {
                IOException("Coordinator socket closed. Code: $code, Reason: $reason")
            }
            cleanup(error)
            logger.e { "[onClosed] Coordinator socket closed. Code: $code, Reason: $reason" }
        }
    }

    // region: API
    /**
     * Disconnect the socket.
     *
     * @param error Optional error that caused the disconnection.
     */
    fun disconnect(): Result<Unit> = internalSocket.close().apply {
        cleanup(null)
        logger.d {
            "[disconnect] Disconnected coordinator socket."
        }
    }

    /**
     * Connect the socket.
     *
     * @param data The connect data to use.
     */
    suspend fun connect(data: ConnectUserData): Either<ConnectionState.Disconnected, ConnectionState.Connected> =
        suspendCancellableCoroutine { continuation ->

            logger.d { "[connect] Connecting to coordinator socket: $data" }

            // Update config with connection data
            config.connectData = config.connectData?.copy(token = data.token)

            // Do initialization work
            init()

            var subscription: StreamSubscription? = null

            // Steps
            val success: (StreamConnectedUser, String) -> Unit = { user, connectionId ->
                subscription?.cancel()
                if (continuation.isActive && !continuation.isCompleted) {
                    connectionState = ConnectionState.Connected(user, connectionId)
                    healthMonitor.start()
                    continuation.resume(
                        Either.Right(
                            ConnectionState.Connected(
                                user,
                                connectionId
                            )
                        )
                    )
                }
            }
            val failure: (Throwable) -> Unit = { exception ->
                subscription?.cancel()
                if (continuation.isActive && !continuation.isCompleted) {
                    connectionState = ConnectionState.Disconnected(exception)
                    continuation.resume(
                        Either.Left(ConnectionState.Disconnected(exception))
                    )
                }
            }

            val apiFailure: (APIError) -> Unit = { error ->
                subscription?.cancel()
                if (continuation.isActive && !continuation.isCompleted) {
                    connectionState = ConnectionState.Disconnected(apiError = error)
                    continuation.resume(
                        Either.Left(ConnectionState.Disconnected(apiError = error))
                    )
                }
            }

            val openSocket: () -> Unit = {
                internalSocket.open(config.socketConfig).recover {
                    logger.e { "[connect] Failed to open coordinator socket. ${it.message}" }
                    continuation.resumeWith(
                        Result.failure(it)
                    )
                }
            }
            val connect: () -> Unit = {
                val authRequest = WSAuthMessageRequest(
                    token = data.token,
                    userDetails = ConnectUserDetailsRequest(
                        id = data.id,
                        name = data.name?.takeUnless { it.isEmpty() },
                        image = data.image?.takeUnless { it.isEmpty() },
                        language = data.language?.takeUnless { it.isEmpty() },
                        invisible = data.invisible,
                        custom = data.custom
                    ),
                )
                coordinatorParser.encode(authRequest).mapCatching {
                    logger.v { "[onOpen] Sending auth request: $it" }
                    internalSocket.send(it.toByteArray())
                }.recover {
                    logger.e(it) { "[onOpen] Failed to serialize auth request. ${it.message}" }
                    failure(it)
                }
            }

            // Subscribe for events
            messageSubscription = internalSocket.subscribe(eventListener).onFailure {
                logger.e { "[connect] Failed to subscribe for events, will not receive `ConnectedEvent`. ${it.message}" }
                failure(it)
            }.getOrNull()

            // Add socket listener that just handles the connect, after which we can remove it
            val socketListener = object : StreamWebSocketListener {
                override fun onOpen(response: Response) {
                    if (response.code == 101) {
                        logger.d { "[onOpen] Coordinator socket opened" }
                        connect()
                    } else {
                        val err =
                            IllegalStateException("Failed to open coordinator socket. Code: ${response.code}")
                        logger.e(err) { "[onOpen] Coordinator socket failed to open. Code: ${response.code}" }
                        failure(err)
                    }
                }

                override fun onMessage(text: String) {
                    logger.v { "[onMessage] Coordinator socket message (string): $text" }
                    coordinatorParser.decode(text.toByteArray()).map { authResponse ->
                        when (authResponse) {
                            // Handle `ConnectedEvent`
                            is ConnectedEvent -> {
                                logger.v { "[onMessage] Handling connected event: $authResponse" }
                                val me = authResponse.me
                                val connectedUser = StreamConnectedUser(
                                    me.createdAt,
                                    me.id,
                                    me.language,
                                    me.role,
                                    me.updatedAt,
                                    me.blockedUserIds ?: emptyList(),
                                    me.teams,
                                    me.custom,
                                    me.deactivatedAt,
                                    me.deletedAt,
                                    me.image,
                                    me.lastActive,
                                    me.name,
                                )
                                connectedEvent = authResponse
                                success(connectedUser, authResponse.connectionId)
                            }

                            // Handle `ConnectionErrorEvent`
                            is ConnectionErrorEvent -> {
                                logger.e { "[onMessage] Coordinator socket connection recoverable error: $authResponse" }
                                apiFailure(authResponse.error)
                            }
                        }
                    }.recover {
                        logger.e(it) { "[onMessage] Failed to deserialize coordinator socket message. ${it.message}" }
                        failure(it)
                    }
                }
            }

            subscription = internalSocket.subscribe(socketListener).onFailure {
                logger.e { "[connect] Failed to subscribe for events, will not receive `ConnectedEvent`. ${it.message}" }
                failure(it)
            }.getOrNull()

            openSocket()
        }
    // region: API end

    private fun init() {

        // Declare health check
        healthMonitor.onInterval {
            logger.v { "[onInterval] Coordinator socket health check" }
            val healthCheckEvent = connectedEvent?.copy()
            if (healthCheckEvent != null) {
                logger.v { "[onInterval] Coordinator socket health check sending: $connectedEvent" }
                coordinatorParser.encode(healthCheckEvent).map { it.toByteArray() }
                    .onSuccess { encodedData ->
                        internalSocket.send(encodedData)
                    }.onFailure {
                        logger.e(it) { "[onInterval] Coordinator socket health check failed. ${it.message}" }
                    }
            } else {
                logger.e { "[onInterval] Coordinator socket health check not run. Connected event is null" }
            }
        }

        healthMonitor.onLivenessThreshold {
            val error = IllegalStateException("Coordinator socket liveness threshold reached")
            logger.e { "[onLivenessThreshold] Coordinator socket liveness threshold reached" }
            cleanup(error)
        }

        // Declare batch processing
        debounceProcessor.start()
        debounceProcessor.onBatch { batch, delay, count ->
            logger.v { "[onBatch] Coordinator socket batch (delay: $delay ms, buffer size: $count): $batch" }
            healthMonitor.ack()
            batch.forEach { message ->
                coordinatorParser.decode(message.toByteArray())
                    .onSuccess { decoded ->
                        subscriptionManager.forEach {
                            it.onEvent(decoded)
                        }
                    }.onFailure {
                        logger.e(it) { "[onBatch] Coordinator socket batch failed. ${it.message}" }
                    }
            }
        }
    }

    private fun cleanup(error: Throwable?) {
        logger.v { "[cleanup] Coordinator socket cleanup (err: ${error?.message})" }
        connectionState = ConnectionState.Disconnected(error)
        healthMonitor.stop()
        debounceProcessor.stop()
        forEach {
            logger.v { "[cleanup] Coordinator socket cleanup calling listener" }
            it.onState(connectionState)
        }
        messageSubscription?.cancel()
        messageSubscription = null
        connectedEvent = null
    }
}