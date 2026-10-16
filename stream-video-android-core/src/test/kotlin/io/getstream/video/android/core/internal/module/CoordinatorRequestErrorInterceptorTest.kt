/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.internal.module

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Test
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLKeyException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException
import kotlin.test.assertFailsWith

class CoordinatorRequestErrorInterceptorTest {

    @Test
    fun `adds the sanitized endpoint to HTTP 4xx and 5xx errors without changing the response`() = runTest {
        listOf(404 to "Not Found", 503 to "Service Unavailable").forEach { (code, reason) ->
            val client = OkHttpClient.Builder()
                .addInterceptor(CoordinatorRequestErrorInterceptor())
                .addInterceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message(reason)
                        .body("backend error".toResponseBody())
                        .build()
                }
                .build()
            val api = Retrofit.Builder()
                .baseUrl("https://video.stream-io-api.com")
                .client(client)
                .build()
                .create(TestCoordinatorApi::class.java)

            val error = assertFailsWith<HttpException> {
                api.joinSuspending(
                    type = "default",
                    id = "call-123",
                    connectionId = "connection-456",
                    apiKey = "api-key-secret",
                    accessToken = "user-token-secret",
                )
            }

            assertThat(error.message).isEqualTo(
                "HTTP $code POST video.stream-io-api.com/video/call/default/call-123/join?" +
                    "connection_id=connection-456 failed: $reason",
            )
            assertThat(error.response()?.code()).isEqualTo(code)
            assertThat(error.response()?.errorBody()?.string()).isEqualTo("backend error")
            assertThat(error.message).contains("call-123")
            assertThat(error.message).contains("connection-456")
            assertThat(error.message).doesNotContain("api_key")
            assertThat(error.message).doesNotContain("access_token")
            assertThat(error.message).doesNotContain("api-key-secret")
            assertThat(error.message).doesNotContain("user-token-secret")
        }
    }

    @Test
    fun `adds the sanitized endpoint and DNS reason to transport failures`() {
        val failure = UnknownHostException("Unable to resolve host video.stream-io-api.com")
        val error = executeWithFailure(failure)

        assertThat(error.message).isEqualTo(
            "POST video.stream-io-api.com/video/call/default/call-123/join?" +
                "connection_id=connection-456 failed: " +
                "DNS lookup failed — Unable to resolve host video.stream-io-api.com",
        )
        assertThat(error.message).contains("connection_id=connection-456")
        assertThat(error.message).doesNotContain("api_key")
        assertThat(error.message).doesNotContain("access_token")
        assertThat(error.message).doesNotContain("api-key-secret")
        assertThat(error.message).doesNotContain("user-token-secret")
        assertThat(error).isInstanceOf(UnknownHostException::class.java)
        assertThat(error.cause).isNull()
        assertThat(error.stackTrace.asList())
            .containsExactlyElementsIn(failure.stackTrace.asList())
            .inOrder()
    }

    @Test
    fun `maps every supported transport failure to a readable message`() {
        val failures = listOf(
            UnknownHostException("Unable to resolve host") to
                "DNS lookup failed — Unable to resolve host",
            SocketTimeoutException("timeout") to
                "request timed out",
            InterruptedIOException("timeout") to
                "request timed out",
            InterruptedIOException("Canceled") to
                "request interrupted — Canceled",
            ConnectException("Connection refused") to
                "connection failed — Connection refused",
            SSLException("Handshake failed") to
                "TLS connection failed — Handshake failed",
            SSLHandshakeException("Handshake failed") to
                "TLS connection failed — Handshake failed",
            SSLKeyException("Invalid key") to
                "TLS connection failed — Invalid key",
            SSLPeerUnverifiedException("Peer not verified") to
                "TLS connection failed — Peer not verified",
            SSLProtocolException("Invalid TLS record") to
                "TLS connection failed — Invalid TLS record",
        )

        failures.forEach { (exception, expectedMessage) ->
            val error = executeWithFailure(exception)

            assertThat(error.message).endsWith("failed: $expectedMessage")
            assertThat(error.javaClass).isEqualTo(exception.javaClass)
            assertThat(error.cause).isNull()
            assertThat(error.stackTrace.asList())
                .containsExactlyElementsIn(exception.stackTrace.asList())
                .inOrder()
        }
    }

    @Test
    fun `preserves the original diagnostic state when recreating an exception`() {
        val originalCause = IOException("socket closed")
        val suppressed = IllegalStateException("cleanup failed")
        val failure = SocketTimeoutException("timeout").apply {
            initCause(originalCause)
            addSuppressed(suppressed)
            bytesTransferred = 42
        }

        val error = executeWithFailure(failure)

        assertThat(error).isNotSameInstanceAs(failure)
        assertThat(error.javaClass).isEqualTo(failure.javaClass)
        assertThat(error.cause).isSameInstanceAs(originalCause)
        assertThat(error.suppressed.asList()).containsExactly(suppressed)
        assertThat(error.stackTrace.asList())
            .containsExactlyElementsIn(failure.stackTrace.asList())
            .inOrder()

        val timeoutError = error as SocketTimeoutException
        assertThat(timeoutError.bytesTransferred).isEqualTo(42)
    }

    @Test
    fun `rethrows unsupported IO exceptions unchanged`() {
        val customTlsFailure = object : SSLException("custom TLS failure") {}
        listOf(
            IOException("unexpected end of stream"),
            IOException(),
            customTlsFailure,
        ).forEach { failure ->
            val error = executeWithFailure(failure)

            assertThat(error).isSameInstanceAs(failure)
        }
    }

    @Test
    fun `adds the URL endpoint to non-Retrofit transport failures`() {
        val failure = UnknownHostException("socket host unavailable")
        val client = clientThrowing(failure)
        val request = Request.Builder()
            .url(
                "https://video.stream-io-api.com/video/connect?" +
                    "connection_id=connection-456&api_key=api-key-secret",
            )
            .build()

        val error = assertFailsWith<UnknownHostException> {
            client.newCall(request).execute()
        }

        assertThat(error.message).isEqualTo(
            "GET video.stream-io-api.com/video/connect?connection_id=connection-456 " +
                "failed: DNS lookup failed — socket host unavailable",
        )
        assertThat(error.message).doesNotContain("api_key")
        assertThat(error.message).doesNotContain("api-key-secret")
        assertThat(error.cause).isNull()
    }

    @Test
    fun `preserves endpoint context when a supported response body read fails`() {
        val failure = SocketTimeoutException("timeout")
        val client = OkHttpClient.Builder()
            .addInterceptor(CoordinatorRequestErrorInterceptor())
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(failingResponseBody(failure))
                    .build()
            }
            .build()
        val request = Request.Builder()
            .url(
                "https://video.stream-io-api.com/video/call/default/call-123?" +
                    "connection_id=connection-456&api_key=api-key-secret",
            )
            .build()

        val response = client.newCall(request).execute()
        val error = assertFailsWith<SocketTimeoutException> {
            response.use { it.body!!.string() }
        }

        assertThat(error.message).isEqualTo(
            "GET video.stream-io-api.com/video/call/default/call-123?" +
                "connection_id=connection-456 failed: request timed out",
        )
        assertThat(error.cause).isNull()
    }

    @Test
    fun `uses endpoint from nested interceptor error when OkHttp reports call timeout`() {
        val intercepted = InterruptedIOException(
            "HEAD hint.stream-io-video.com/ failed: request interrupted — Canceled",
        )
        val timeout = InterruptedIOException("timeout").apply { initCause(intercepted) }

        val error = timeout.restoreCoordinatorRequestContextFromCause()

        assertThat(error.message).isEqualTo(
            "HEAD hint.stream-io-video.com/ failed: request timed out",
        )
        assertThat(error.cause).isSameInstanceAs(intercepted)
    }

    private fun executeWithFailure(failure: IOException): IOException {
        val client = clientThrowing(failure)
        val api = Retrofit.Builder()
            .baseUrl("https://video.stream-io-api.com")
            .client(client)
            .build()
            .create(TestCoordinatorApi::class.java)

        return assertFailsWith {
            api.join(
                type = "default",
                id = "call-123",
                connectionId = "connection-456",
                apiKey = "api-key-secret",
                accessToken = "user-token-secret",
            ).execute()
        }
    }

    private fun clientThrowing(failure: IOException): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(CoordinatorRequestErrorInterceptor())
        .addInterceptor(
            Interceptor {
                throw failure
            },
        )
        .build()

    private fun failingResponseBody(failure: IOException) =
        object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long = throw failure

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() = Unit
        }.buffer().asResponseBody()

    private interface TestCoordinatorApi {
        @POST("/video/call/{type}/{id}/join")
        fun join(
            @Path("type") type: String,
            @Path("id") id: String,
            @Query("connection_id") connectionId: String,
            @Query("api_key") apiKey: String,
            @Query("access_token") accessToken: String,
        ): Call<Unit>

        @POST("/video/call/{type}/{id}/join")
        suspend fun joinSuspending(
            @Path("type") type: String,
            @Path("id") id: String,
            @Query("connection_id") connectionId: String,
            @Query("api_key") apiKey: String,
            @Query("access_token") accessToken: String,
        )
    }
}
