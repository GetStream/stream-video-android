package io.getstream.video.android.core

import android.content.Context
import android.net.ConnectivityManager
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.CoordinatorSocket
import io.getstream.video.android.core.socket.SfuSocket
import io.getstream.video.android.core.socket.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit


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
 * @see SFUConnectionModule
 * @see FiniteStateMachine
 * @see SocketStateService
 *
 * TODO:
 * - swap ConnectionModule / SFUSocket setup
 * - authenticated continuation or different approach
 * - Lots more testing
 */
@RunWith(RobolectricTestRunner::class)
class SocketTest: TestBase() {
    val coordinatorUrl = "https://video.stream-io-api.com/video/connect?api_key=hd8szvscpxvd&stream-auth-type=jwt&X-Stream-Client=stream-video-android"
    val sfuUrl = "https://sfu-039364a.lsw-ams1.stream-io-video.com/ws?api_key=hd8szvscpxvd"

    val sfuToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI4ZDBlYjU0NDg4ZDFiYTUxOTk3Y2Y1NWRmYTY0Y2NiMCIsInN1YiI6InVzZXIvdGhpZXJyeSIsImF1ZCI6WyJzZnUtMDM5MzY0YS5sc3ctYW1zMS5zdHJlYW0taW8tdmlkZW8uY29tIl0sImV4cCI6MTY4MTM4MTUzOSwibmJmIjoxNjgxMzU5OTM5LCJpYXQiOjE2ODEzNTk5MzksImFwcF9pZCI6MTEyOTUyOCwiY2FsbF9pZCI6ImRlZmF1bHQ6ZmZlZDM5MDgtYTM0Ni00ZjM5LTg4MWYtZjJkMWNjOGM4YTE5IiwidXNlciI6eyJpZCI6InRoaWVycnkiLCJuYW1lIjoiVGhpZXJyeSIsImltYWdlIjoiaGVsbG8iLCJ0cCI6IjJ0T21iVVZoQVNSdytnaDNFM2hheWJTZEpwdUVwTWk0In0sInJvbGVzIjpbInVzZXIiXSwib3duZXIiOnRydWV9.9MAI0if2Uxc-m7pu56bSPxpoP_Yu8TB-QVd3187NmA4v_O1uoRkWPNV-l-ieTgUoqKsJtncvmgG0Xts0W7nSMw"

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
        val scope = CoroutineScope(DispatcherProvider.IO)
        val socket = CoordinatorSocket(coordinatorUrl, testData.users["thierry"]!!, testData.tokens["thierry"]!!, scope , buildOkHttp(), networkStateProvider)
        socket.connect()

        launch {
            println("watching events")
            socket.events.collect() {
                println("event: $it")
            }
        }

        launch {
            println("watching errors")
            socket.errors.collect() {
                throw it
            }
        }
        // wait for the socket to connect (connect response or error)

    }

    @Test
    fun `sfu socket`() = runTest {
        val networkStateProvider = NetworkStateProvider(
            connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
        val sessionId = randomUUID().toString()
        val updateSdp:  () -> String = {
            "hello"
        }
        val scope = CoroutineScope(DispatcherProvider.IO)
        val socket = SfuSocket(sfuUrl, sessionId, sfuToken, updateSdp, scope, buildOkHttp(), networkStateProvider)
        socket.connect()

        launch {
            println("watching events")
            socket.events.collect() {
                println("event: $it")
            }
        }

        launch {
            println("watching errors")
            socket.errors.collect() {
                throw it
            }
        }
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