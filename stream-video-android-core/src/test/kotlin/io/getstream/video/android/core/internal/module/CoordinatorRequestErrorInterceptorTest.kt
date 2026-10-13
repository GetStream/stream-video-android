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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
                api.joinSuspending("default", "abc", "secret")
            }

            assertThat(error.message).isEqualTo(
                "HTTP $code POST video.stream-io-api.com/video/call/{type}/{id}/join failed: $reason",
            )
            assertThat(error.response()?.code()).isEqualTo(code)
            assertThat(error.response()?.errorBody()?.string()).isEqualTo("backend error")
            assertThat(error.message).doesNotContain("abc")
            assertThat(error.message).doesNotContain("secret")
        }
    }

    @Test
    fun `adds the sanitized endpoint and DNS reason to transport failures`() {
        val error = executeWithFailure(
            UnknownHostException("Unable to resolve host video.stream-io-api.com"),
        )

        assertThat(error.message).isEqualTo(
            "POST video.stream-io-api.com/video/call/{type}/{id}/join failed: " +
                "DNS lookup failed — Unable to resolve host video.stream-io-api.com",
        )
        assertThat(error.message).doesNotContain("connection_id")
        assertThat(error.cause).isInstanceOf(UnknownHostException::class.java)
    }

    @Test
    fun `identifies timeout failures`() {
        val error = executeWithFailure(SocketTimeoutException("timeout"))

        assertThat(error.message).endsWith("failed: request timed out — timeout")
    }

    @Test
    fun `adds the URL endpoint to non-Retrofit transport failures`() {
        val failure = UnknownHostException("socket host unavailable")
        val client = clientThrowing(failure)
        val request = Request.Builder().url("https://video.stream-io-api.com/video/connect").build()

        val error = assertFailsWith<CoordinatorRequestException> {
            client.newCall(request).execute()
        }

        assertThat(error.message).isEqualTo(
            "GET video.stream-io-api.com/video/connect failed: DNS lookup failed — socket host unavailable",
        )
        assertThat(error.cause).isSameInstanceAs(failure)
    }

    @Test
    fun `uses endpoint from nested interceptor error when OkHttp reports call timeout`() {
        val request = Request.Builder().url("https://hint.stream-io-video.com/").head().build()
        val intercepted = CoordinatorRequestException(request, IOException("Canceled"))
        val timeout = InterruptedIOException("timeout").apply { initCause(intercepted) }

        val error = timeout.withCoordinatorRequestContext()

        assertThat(error.message).isEqualTo(
            "HEAD hint.stream-io-video.com/ failed: request timed out — timeout",
        )
        assertThat(error.cause).isSameInstanceAs(timeout)
    }

    private fun executeWithFailure(failure: IOException): CoordinatorRequestException {
        val client = clientThrowing(failure)
        val api = Retrofit.Builder()
            .baseUrl("https://video.stream-io-api.com")
            .client(client)
            .build()
            .create(TestCoordinatorApi::class.java)

        return assertFailsWith { api.join("default", "abc", "secret").execute() }
    }

    private fun clientThrowing(failure: IOException): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(CoordinatorRequestErrorInterceptor())
        .addInterceptor(
            Interceptor {
                throw failure
            },
        )
        .build()

    private interface TestCoordinatorApi {
        @POST("/video/call/{type}/{id}/join")
        fun join(
            @Path("type") type: String,
            @Path("id") id: String,
            @Query("connection_id") connectionId: String,
        ): Call<Unit>

        @POST("/video/call/{type}/{id}/join")
        suspend fun joinSuspending(
            @Path("type") type: String,
            @Path("id") id: String,
            @Query("connection_id") connectionId: String,
        )
    }
}
