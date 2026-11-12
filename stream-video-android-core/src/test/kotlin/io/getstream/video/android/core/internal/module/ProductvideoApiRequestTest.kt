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
import io.getstream.android.video.api.generated.models.SortParamRequest
import io.getstream.android.video.internal.generated.apis.ProductvideoApi
import io.getstream.android.video.internal.generated.infrastructure.JsonQuery
import io.getstream.android.video.internal.generated.infrastructure.QueryConverterFactory
import io.getstream.android.video.internal.generated.infrastructure.Serializer
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Checks what [ProductvideoApi] puts on the wire through the converters [CoordinatorConnectionModule] registers.
 */
class ProductvideoApiRequestTest {

    private lateinit var sent: Request

    private val api: ProductvideoApi = Retrofit.Builder()
        .baseUrl("https://video.example.com/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        sent = chain.request()
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(500)
                            .message("Error")
                            .body("".toResponseBody())
                            .build()
                    },
                )
                .build(),
        )
        // The same converters, in the same order, as CoordinatorConnectionModule.
        .addConverterFactory(QueryConverterFactory(Serializer.moshi))
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
        .build()
        .create(ProductvideoApi::class.java)

    @Test
    fun `sends JSON query parameters as one JSON string each`() = runTest {
        send {
            queryCallSessionParticipantStats(
                callType = "default",
                callId = "call",
                session = "session",
                sort = JsonQuery(listOf(SortParamRequest(direction = -1, field = "joined_at"))),
                filterConditions = JsonQuery(mapOf("user_id" to mapOf("\$eq" to "alice"))),
            )
        }

        assertThat(sent.url.queryParameterValues("sort"))
            .containsExactly("""[{"direction":-1,"field":"joined_at"}]""")
        assertThat(sent.url.queryParameterValues("filter_conditions"))
            .containsExactly("""{"user_id":{"${'$'}eq":"alice"}}""")
    }

    @Test
    fun `sends the participant sessions filter as one JSON string`() = runTest {
        send {
            queryCallParticipantSessions(
                type = "default",
                id = "call",
                session = "session",
                filterConditions = JsonQuery(mapOf("user_id" to "alice")),
            )
        }

        assertThat(
            sent.url.queryParameterValues("filter_conditions"),
        ).containsExactly("""{"user_id":"alice"}""")
    }

    @Test
    fun `omits JSON query parameters that are not set`() = runTest {
        send {
            queryCallSessionParticipantStats(
                callType = "default",
                callId = "call",
                session = "session",
            )
        }

        assertThat(sent.url.queryParameterNames).containsNoneOf("sort", "filter_conditions")
    }

    @Test
    fun `sends an empty JSON object when stopping a recording`() = runTest {
        send { stopRecording(type = "default", id = "call", recordingType = "composite") }

        assertThat(sent.method).isEqualTo("POST")
        assertThat(Buffer().also { sent.body!!.writeTo(it) }.readUtf8()).isEqualTo("{}")
    }

    // The interceptor answers every request with an error, so only an HttpException means the request was sent.
    private suspend fun send(call: suspend ProductvideoApi.() -> Any) {
        try {
            api.call()
        } catch (_: HttpException) {
        }
    }
}
