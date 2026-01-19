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

package io.getstream.video.android.core.call.connection.utils

import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.errors.RtcException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiCallsTest {

    @Test
    fun `returns Success when apiCall succeeds`() = runTest {
        val result = wrapAPICall { "OK" }

        assertIs<Success<String>>(result)
        assertEquals("OK", result.value)
    }

    @Test
    fun `returns Failure when HttpException occurs`() = runTest {
        val exception = mockHttpException(404)
        val result = wrapAPICall<String> { throw exception }

        assertIs<Failure>(result)
        val error = result.value as io.getstream.result.Error.ThrowableError
        assertEquals("CallClientImpl error needs to be handled", error.message)
        assertEquals(exception, error.cause)
    }

    @Test
    fun `returns Failure when RtcException occurs`() = runTest {
        val rtcException = RtcException("RTC Failed", RuntimeException())
        val result = wrapAPICall<String> { throw rtcException }

        assertIs<Failure>(result)
        val error = result.value as io.getstream.result.Error.ThrowableError
        assertEquals("RTC Failed", error.message)
        assertEquals(rtcException, error.cause)
    }

    @Test
    fun `returns Failure when IOException occurs`() = runTest {
        val ioException = IOException("No Internet")
        val result = wrapAPICall<String> { throw ioException }

        assertIs<Failure>(result)
        val error = result.value as io.getstream.result.Error.ThrowableError
        assertEquals("No Internet", error.message)
        assertEquals(ioException, error.cause)
    }

    // --- Helpers ---

    private fun mockHttpException(code: Int): HttpException {
        val response = retrofit2.Response.error<String>(code, okhttp3.ResponseBody.create(null, ""))
        return HttpException(response)
    }
}
