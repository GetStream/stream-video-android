package io.getstream.video.android.core

import android.content.Context
import android.net.ConnectivityManager
import com.squareup.moshi.JsonAdapter
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.call.signal.socket.SfuSocketImpl
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.errors.create
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.socket.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.ConnectUserDetailsRequest
import org.openapitools.client.models.WSAuthMessageRequest
import org.robolectric.RobolectricTestRunner
import stream.video.coordinator.client_v1_rpc.WebsocketHealthcheck
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


public sealed class SocketState {
    object NotConnected : SocketState() {
        override fun toString(): String = "Not Connected"
    }

    object Connecting : SocketState() {
        override fun toString(): String = "Connecting"
    }

    data class Connected(val event: VideoEvent) : SocketState()

    object NetworkDisconnected : SocketState() {
        override fun toString(): String = "NetworkDisconnected"
    }

    data class DisconnectedTemporarily(val error: Error.NetworkError?) : SocketState()
    data class DisconnectedPermanently(val error: Error.NetworkError?) : SocketState()
    object DisconnectedByRequest : SocketState() {
        override fun toString(): String = "DisconnectedByRequest"
    }
}

/**
 * The Coordinator sockets send a user authentication request
 *  @see WSAuthMessageRequest
 *
 */
public class PersistentCoordinatorSocket(
    private val url: String,
    private val httpClient: OkHttpClient,
    private val user: User,
    private val token: String,
    private val networkStateProvider: NetworkStateProvider
): PersistentSocket(
    url=url,
    httpClient=httpClient,
    user=user,
    token=token,
    networkStateProvider=networkStateProvider
) {

}

/**
 * The SFU socket is slightly different from the coordinator socket
 * It sends a JoinRequest to authenticate
 */
public class PersistentSFUSocket(
    private val url: String,
    private val httpClient: OkHttpClient,
    private val user: User,
    private val token: String,
    private val networkStateProvider: NetworkStateProvider
): PersistentSocket(
    url=url,
    httpClient=httpClient,
    user=user,
    token=token,
    networkStateProvider=networkStateProvider
) {

}

/**
 * PersistentSocket architecture
 *
 * - Healthmonitor that sends a ping every 30 seconds
 * - Automatically reconnects if it encounters a temp failure
 * - Raises the error if there is a permanent failure
 * - Flow to avoid concurrency related bugs
 * - Ability to wait till the socket is connected (important to prevent race conditions)
 */
open class PersistentSocket(
    /** The URL to connect to */
    private val url: String,
    /** Inject your http client */
    private val httpClient: OkHttpClient,
    /** The user for authentication */
    private val user: User,
    /** The token for authentication */
    private val token: String,
    /** Inject your network state provider */
    private val networkStateProvider: NetworkStateProvider,
    /** Set the scope everything should run in */
    private val scope : CoroutineScope = CoroutineScope(DispatcherProvider.IO)
) : WebSocketListener() {
    private val logger by taggedLogger("PersistentSocket")

    /** flow with all the events, listen to this */
    val events = MutableSharedFlow<VideoEvent>()

    val _connectionState = MutableStateFlow<SocketState>(SocketState.NotConnected)
    /** the current connection state of the socket */
    val connectionState: StateFlow<SocketState> = _connectionState

    /** the connection id */
    var connectionId: String = ""

    /** Continuation if the socket successfully connected */
    lateinit var connected : Continuation<Unit>

    private lateinit var socket: WebSocket

    // prevent us from resuming the continuation twice
    private var continuationCompleted: Boolean = false
    // we don't raise errors if you're closing the connection yourself
    private var closedByClient: Boolean = false

    private companion object {
        // TODO: little delay on reconnect
        private const val DEFAULT_DELAY = 500
    }

    /**
     * Connect the socket, authenticate, start the healthmonitor and see if the network is online
     */
    suspend fun connect() = suspendCoroutine<Unit> {continuation ->
        connected = continuation
        _connectionState.value = SocketState.Connecting
        // step 1 create the socket
        socket = createSocket()
        // step 2 authenticate the user/call etc
        authenticate()
        // step 3 monitor for health every 30 seconds
        healthMonitor.start()
        // also monitor if we are offline/online
        networkStateProvider.subscribe(networkStateListener)
    }

    /**
     * Disconnect the socket
     */
    fun disconnect() {
        closedByClient = true
        continuationCompleted = false
        _connectionState.value = SocketState.DisconnectedByRequest
        socket?.close(EventsParser.CODE_CLOSE_SOCKET_FROM_CLIENT, "Connection close by client")
        connectionId = ""
        healthMonitor.stop()
        networkStateProvider.unsubscribe(networkStateListener)
    }

    /**
     * Increment the reconnection attempts, disconnect and reconnect
     */
    suspend fun reconnect() {
        reconnectionAttempts++
        disconnect()
        connect()
    }

    fun authenticate() {
        logger.d { "[authenticateUser] user: $user" }

        // TODO: handle guest and anon users

        if (token.isEmpty()) {
            throw IllegalStateException("User token is empty")
        }

        val adapter: JsonAdapter<WSAuthMessageRequest> =
            Serializer.moshi.adapter(WSAuthMessageRequest::class.java)

        val authRequest = WSAuthMessageRequest(
            token = token,
            userDetails = ConnectUserDetailsRequest(
                id = user.id,
                name = user.name,
                image = user.imageUrl,
            )
        )
        val message = adapter.toJson(authRequest)

        socket.send(message)
    }

    private val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override fun onConnected() {
            val state = connectionState.value
            logger.i { "[onNetworkConnected] state: $state" }
            if (state is SocketState.DisconnectedTemporarily || state == SocketState.NetworkDisconnected) {
                scope.launch {
                    reconnect()
                }
            }
        }

        override fun onDisconnected() {
            logger.i { "[onNetworkDisconnected] state: $connectionState.value" }
            if (connectionState.value is SocketState.Connected || connectionState.value is SocketState.Connecting) {
                _connectionState.value = SocketState.NetworkDisconnected
            }
        }
    }

    private var reconnectionAttempts = 0

    fun createSocket(): WebSocket {
        logger.d { "[createSocket] url: $url" }

        val request = Request
            .Builder()
            .url(url)
            .addHeader("Connection", "Upgrade")
            .addHeader("Upgrade", "websocket")
            .build()

        return httpClient.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.d { "[onOpen] response: $response" }
        if (!continuationCompleted) {
            connected.resume(Unit)
            continuationCompleted = true
        }


    }

    /** Invoked when a text (type `0x1`) message has been received. */
    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.d { "[onMessage] text: $text " }

        scope.launch {
            // parse the message
            val data = Json.decodeFromString<JsonObject>(text)
            val eventType = EventType.from(data["type"]?.jsonPrimitive?.content!!)
            val processedEvent = EventMapper.mapEvent(eventType, text)

            if (processedEvent is ConnectedEvent) {
                // TODO: rename when we fix event parsing
                connectionId = processedEvent.clientId
                _connectionState.value = SocketState.Connected(processedEvent)
            }

            logger.d { "parsed event $processedEvent" }

            // emit the message
            events.emit(processedEvent)
        }
    }

    /** Invoked when a binary (type `0x2`) message has been received. */
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        logger.d { "[onMessage] bytes: $bytes" }
        TODO("Binary messages are not supported")
    }

    /**
     * Invoked when the remote peer has indicated that no more incoming messages will be transmitted.
     */
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logger.d { "[onClosing] code: $code, reason: $reason" }
    }

    /**
     * Invoked when both peers have indicated that no more messages will be transmitted and the
     * connection has been successfully released. No further calls to this listener will be made.
     */
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logger.d { "[onClosed] code: $code, reason: $reason" }
        if (code == EventsParser.CODE_CLOSE_SOCKET_FROM_CLIENT) {
            closedByClient = true
        } else {
            // Treat as failure and reconnect, socket shouldn't be closed by server
            // TODO: how do we expose errors to the user?
        }
    }


    /**
     * Invoked when a web socket has been closed due to an error reading from or writing to the
     * network. Both outgoing and incoming messages may have been lost. No further calls to this
     * listener will be made.
     */
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.d { "[onFailure] t: $t, response: $response" }
        if (!continuationCompleted) {
            connected.resumeWithException(t)
            continuationCompleted = true
        }

        // TODO: how to expose this error?
    }

    private val healthMonitor = HealthMonitor(object : HealthMonitor.HealthCallback {
        override fun reconnect() {
            val state = connectionState.value
            if (state is SocketState.DisconnectedTemporarily) {
                scope.launch {
                    this@PersistentSocket.reconnect()
                }
            }
        }

        override fun check() {
            val state = connectionState.value
            (state as? SocketState.Connected)?.let {
                val state = WebsocketHealthcheck(
                    user_id = user.id,
                    client_id = "client_id",
                )
                socket.send(state.encodeByteString())
            }
        }
    })


}



/**
 * Coordinator socket URL
 *
 * * https://video.stream-io-api.com/video/connect?api_key=hd8szvscpxvd&stream-auth-type=jwt&X-Stream-Client=stream-video-android
 * *
 *
 * SFU Socket URL
 *
 * * https://sfu-000c954.fdc-ams1.stream-io-video.com/ws?api_key=hd8szvscpxvd
 * * SFU Token:
  *
 * @see ConnectionModule
 * @see SfuSocketImpl
 * @see VideoSocketImpl
 * @see EventsParser
 * @see SignalEventsParser
 * @see EventMapper
 * @see SocketFactory
 * @see Socket
 *
 * TODO:
 * - how to expose errors to the user, both temporary and permanent ones
 *
 */
@RunWith(RobolectricTestRunner::class)
class SocketTest: TestBase() {
    val coordinatorUrl = "https://video.stream-io-api.com/video/connect?api_key=hd8szvscpxvd&stream-auth-type=jwt&X-Stream-Client=stream-video-android"
    val sfuUrl = "https://sfu-000c954.fdc-ams1.stream-io-video.com/ws?api_key=hd8szvscpxvd"
    val sfuToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI4ZDBlYjU0NDg4ZDFiYTUxOTk3Y2Y1NWRmYTY0Y2NiMCIsInN1YiI6InVzZXIvdGhpZXJyeSIsImF1ZCI6WyJzZnUtMDAwYzk1NC5mZGMtYW1zMS5zdHJlYW0taW8tdmlkZW8uY29tIl0sImV4cCI6MTY4MTM0MTI2NCwibmJmIjoxNjgxMzE5NjY0LCJpYXQiOjE2ODEzMTk2NjQsImFwcF9pZCI6MTEyOTUyOCwiY2FsbF9pZCI6ImRlZmF1bHQ6NzM4NzdjOTYtYTgyZC00ZmUzLTkzY2YtNWYwOWI1NzdiZTExIiwidXNlciI6eyJpZCI6InRoaWVycnkiLCJuYW1lIjoiVGhpZXJyeSIsImltYWdlIjoiaGVsbG8iLCJ0cCI6IksyeE9qK3ZEQU5DTGVGTU5XMnpvTStaQ2ZDcWV5Y1VnIn0sInJvbGVzIjpbInVzZXIiXSwib3duZXIiOnRydWV9.QSNRDYkQUxMNZmtUXOAQ8yEJohiv-tCbKYN3ix5w3ApS47JPHhNCcKkOLerZ1VfJJ4xdl-POFHCPnaWHdF5d-g"

    fun buildOkHttp(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        return OkHttpClient.Builder()
//            .addInterceptor(
//                buildCredentialsInterceptor(
//                    interceptorWrapper = interceptorWrapper
//                )
//            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            //.addInterceptor(buildHostSelectionInterceptor(interceptorWrapper = interceptorWrapper))
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .build()
    }

    @Test
    fun `connect the socket`()  = runTest {
        val networkStateProvider = NetworkStateProvider(
            connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
        val socket = PersistentCoordinatorSocket(coordinatorUrl, buildOkHttp(), testData.users["thierry"]!!, testData.tokens["thierry"]!!, networkStateProvider)
        socket.connect()
        runBlocking {
            delay(10000L)
        }

        // wait for the socket to connect (connect response or error)

    }

    @Test
    fun `if we get a temporary error we should retry`() = runTest {

    }

    @Test
    fun `a permanent error shouldn't be retried`() = runTest {

    }

    @Test
    fun `error parsing`() = runTest {

    }

}