/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.internal.network

import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.randomString
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.token.FakeTokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TokenAuthInterceptorTest {

    private val testToken = randomString()
    private val tokenManager: FakeTokenManager = spyk(FakeTokenManager(testToken))
    private lateinit var chain: Interceptor.Chain
    private lateinit var tokenAuthInterceptor: TokenAuthInterceptor
    private val authType = randomString()
    private val format = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val request: Request = Request.Builder()
        .url("https://api.stream.io/video")
        .build()

    @Before
    fun setup() {
        chain = mockk {
            every { request() } returns request
        }

        tokenAuthInterceptor = TokenAuthInterceptor(tokenManager) { authType }
    }

    @Test
    fun `test adds auth headers to request`() {
        val requestSlot = slot<Request>()

        every { chain.proceed(capture(requestSlot)) } returns Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        tokenAuthInterceptor.intercept(chain)

        val capturedRequest = requestSlot.captured
        assertEquals(testToken, capturedRequest.header("Authorization"))
        assertEquals(authType, capturedRequest.header("stream-auth-type"))
        verify { chain.proceed(any()) }
    }

    @Test
    fun `test handles auth error by refreshing token`() {
        val argumentsCaptor: MutableList<Request> = mutableListOf()

        val errorResponse = ErrorResponse(
            message = "Token invalid",
            details = emptyList(),
            statusCode = 401,
            code = VideoErrorCode.AUTHENTICATION_ERROR.code,
        )
        val errorBody = format.encodeToString(errorResponse)

        val errorResponseObj = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(errorBody.toResponseBody("application/json".toMediaType()))
            .build()

        val successResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        every {
            chain.proceed(capture(argumentsCaptor))
        } returnsMany listOf(errorResponseObj, successResponse)

        val response = tokenAuthInterceptor.intercept(chain)

        val firstRequest = argumentsCaptor.first()
        val secondRequest = argumentsCaptor.drop(1).first()

        assertEquals(testToken, firstRequest.header("Authorization"))
        assertEquals(authType, firstRequest.header("stream-auth-type"))

        assertEquals(testToken, secondRequest.header("Authorization"))
        assertEquals(authType, secondRequest.header("stream-auth-type"))

        verify(exactly = 2) { chain.proceed(any()) }
        assert(response.code == 200)
    }

    @Test
    fun `test successful response doesn't refresh token`() {
        val requestSlot = slot<Request>()

        val successResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        every { chain.proceed(capture(requestSlot)) } returns successResponse

        val response = tokenAuthInterceptor.intercept(chain)

        val capturedRequest = requestSlot.captured
        assertEquals(testToken, capturedRequest.header("Authorization"))
        assertEquals(authType, capturedRequest.header("stream-auth-type"))

        verify(exactly = 1) { chain.proceed(any()) }
        assert(response.code == 200)
    }

    @Test
    fun `test non-auth error doesn't refresh token`() {
        val requestSlot = slot<Request>()

        val errorResponse = ErrorResponse(
            message = "Not found",
            details = emptyList(),
            statusCode = 404,
            code = 404,
        )
        val errorBody = format.encodeToString(errorResponse)

        val notFoundResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body(errorBody.toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.proceed(capture(requestSlot)) } returns notFoundResponse

        val response = tokenAuthInterceptor.intercept(chain)

        val capturedRequest = requestSlot.captured
        assertEquals(testToken, capturedRequest.header("Authorization"))
        assertEquals(authType, capturedRequest.header("stream-auth-type"))

        verify(exactly = 1) { chain.proceed(any()) }
        assert(response.code == 404)
    }

    @Test
    fun `test invalid error response format doesn't trigger refresh`() {
        val requestSlot = slot<Request>()

        val invalidErrorBody = "{ invalid json }"

        val badResponse = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Server Error")
            .body(invalidErrorBody.toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.proceed(capture(requestSlot)) } returns badResponse

        val response = tokenAuthInterceptor.intercept(chain)

        val capturedRequest = requestSlot.captured
        assertEquals(testToken, capturedRequest.header("Authorization"))
        assertEquals(authType, capturedRequest.header("stream-auth-type"))

        verify(exactly = 1) { chain.proceed(any()) }
        assert(response.code == 500)
    }
}
