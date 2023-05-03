/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package org.openapitools.client

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.Before
import org.junit.Test
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.APIError
import org.openapitools.client.models.APIError.Code.*
import java.lang.String.format
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

sealed class TestError(val msg: String) {
    override fun toString(): String = msg

    companion object {
        fun fromString(s: String): TestError = when (s) {
            "internal-server-error" -> InternalServerError
            else -> UnknownError(s)
        }
    }

    object InternalServerError : TestError("internal-server-error")
    data class UnknownError(val code: String) : TestError(code)

    class TestErrorJsonAdapter : JsonAdapter<TestError>() {
        @FromJson
        override fun fromJson(reader: JsonReader): TestError? {
            val s = reader.nextString() ?: return null
            return fromString(s)
        }

        @ToJson
        override fun toJson(writer: JsonWriter, value: TestError?) {
            writer.value(value?.msg)
        }
    }
}

sealed class TestErrorV2(val msg: String) {
    override fun toString(): String = msg

    companion object {
        fun fromString(s: String): TestErrorV2 = when (s) {
            "internal-server-error" -> InternalServerError
            "security-error" -> SecurityError
            else -> UnknownError(s)
        }
    }

    object InternalServerError : TestErrorV2("internal-server-error")
    object SecurityError : TestErrorV2("security-error")
    data class UnknownError(val code: String) : TestErrorV2(code)

    class TestErrorV2JsonAdapter : JsonAdapter<TestErrorV2>() {
        @FromJson
        override fun fromJson(reader: JsonReader): TestErrorV2? {
            val s = reader.nextString() ?: return null
            return fromString(s)
        }

        @ToJson
        override fun toJson(writer: JsonWriter, value: TestErrorV2?) {
            writer.value(value?.msg)
        }
    }
}

class JsonSerializeDeserializeTest {
    lateinit var adapter: JsonAdapter<TestError>
    lateinit var adapterV2: JsonAdapter<TestErrorV2>

    @Before
    fun setupMoshi() {
        val moshi = Moshi.Builder()
                .add(TestError.TestErrorJsonAdapter())
                .add(TestErrorV2.TestErrorV2JsonAdapter())
                .build()
        adapter = moshi.adapter(TestError::class.java)
        adapterV2 = moshi.adapter(TestErrorV2::class.java)
    }

    @Test
    fun `test serialization of known to json`() {
        val json = adapter.toJson(TestError.InternalServerError)
        assertEquals("\"internal-server-error\"", json)
    }

    @Test
    fun `test deserialization of known from json`() {
        val obj = adapter.fromJson("\"internal-server-error\"")
        assertTrue { obj is TestError.InternalServerError }
    }

    @Test
    fun `test serialization of unknown to json`() {
        val json = adapter.toJson(TestError.UnknownError("security-error"))
        assertEquals("\"security-error\"", json)
    }

    @Test
    fun `test deserialization of unknown from json`() {
        val obj = adapter.fromJson("\"security-error\"")
        when (obj) {
            is TestError.UnknownError -> assertEquals("security-error", obj.code)
            else -> fail("expected UnknownError kind")
        }
    }

    @Test
    fun `test compatibility across versions for known`() {
        assertTrue { adapter.fromJson(adapterV2.toJson(TestErrorV2.InternalServerError)) is TestError.InternalServerError }
        assertTrue { adapterV2.fromJson(adapter.toJson(TestError.InternalServerError)) is TestErrorV2.InternalServerError }
    }

    @Test
    fun `test unknown to lower, but known to higher should not fail`() {
        assertTrue { adapter.fromJson(adapterV2.toJson(TestErrorV2.SecurityError))?.
            equals(TestError.UnknownError("security-error"))!! }
        assertTrue { adapterV2.fromJson(adapter.toJson(TestError.UnknownError("security-error")))?.
            equals(TestErrorV2.SecurityError)!! }
    }

    @Test
    fun `test unknown for both lower and higher versions should not fail`() {
        val errorCode = "rate-limited"
        assertTrue { adapter.fromJson(adapterV2.toJson(TestErrorV2.UnknownError(errorCode)))?.
            equals(TestError.UnknownError(errorCode))!! }
        assertTrue { adapterV2.fromJson(adapter.toJson(TestError.UnknownError(errorCode)))?.
            equals(TestErrorV2.UnknownError(errorCode))!! }
    }

    @Test
    fun `test unknown enum values are preserved while marking unknown and known values are correctly mapped`() {
        val moshi = Serializer.moshi

        // known field value
        val apiError = APIError(
            statusCode = 500,
            code = InternalError,
            details = emptyList(),
            duration = "1s",
            message = "something went wrong",
            moreInfo = "server-side error",
            exceptionFields = null,
        )
        val generatedJson = """{"StatusCode":500,"code":"internal-error","details":[],"duration":"1s","message":"something went wrong","more_info":"server-side error"}"""
        assertEquals(apiError, moshi.adapter(APIError::class.java).fromJson(generatedJson))

        // unknown field value
        val apiUnknownError = APIError(
            statusCode = 1500,
            code = Unknown("some-weird-error"),
            details = emptyList(),
            duration = "1s",
            message = "something went wrong",
            moreInfo = "server-side error",
            exceptionFields = null,
        )
        val generatedJsonWithUnknownError = """{"StatusCode":1500,"code":"some-weird-error","details":[],"duration":"1s","message":"something went wrong","more_info":"server-side error"}"""
        assertEquals(apiUnknownError, moshi.adapter(APIError::class.java).fromJson(generatedJsonWithUnknownError))
    }

    @Test
    fun `illustrate APIError usage in the context of retries`() {
        val jsonTemplate = """{"StatusCode":500,"code":"%s","details":[],"duration":"1s","message":"something went wrong","more_info":"server-side error"}"""

        val internalServerError = format(jsonTemplate, "internal-error")
        val rateLimited = format(jsonTemplate, "rate-limited")
        val expiredToken = format(jsonTemplate, "expired-token")
        val authFailed = format(jsonTemplate, "auth-failed")
        val unknownErr = format(jsonTemplate, "unknown-weird-error")

        assertEquals(true, isRetriable(parseJson(internalServerError)))
        assertEquals(true, isRetriable(parseJson(rateLimited)))
        assertEquals(true, isRetriable(parseJson(expiredToken)))
        assertEquals(false, isRetriable(parseJson(authFailed)))
        assertEquals(null, isRetriable(parseJson(unknownErr)))
    }

    private fun parseJson(json: String): APIError.Code {
        val moshi = Serializer.moshi
        return moshi.adapter(APIError::class.java).fromJson(json)!!.code
    }

    private fun isRetriable(code: APIError.Code): Boolean? {
        return when (code) {
            AccessKeyError -> false
            AppSuspended -> false
            AuthFailed -> false
            ChannelFeatureNotSupported -> false
            ConnectionIdNotFound -> false
            CoolDown -> true
            CustomCommandEndpointEqualCallError -> false
            CustomCommandEndpointMissing -> false
            DuplicateUsername -> false
            EventNotSupported -> false
            InputError -> false
            InvalidTokenSignature -> false
            MessageTooLong -> false
            ModerationFailed -> false
            MultipleNestingLevel -> false
            NotAllowed -> false
            NotFound -> false
            NotSupportedInPushV1 -> false
            PayloadTooBig -> false
            QueryCallsPermissionsMismatch -> false
            QueryChannelPermissionsMismatch -> false
            TokenNotValidYet -> true // but only after some time
            TokenUsedBeforeIat -> true // but only after some time
            TooManyConnections -> true // by closing a few connections
            VideoCreateCallFailed -> true // maybe could not contact SFU
            VideoInvalidCallId -> false
            VideoJoinCallFailure -> true // maybe could not contact SFU
            VideoNoDatacentersAvailable -> false
            VideoProviderNotConfigured -> false
            ExpiredToken -> true
            InternalError -> true
            RateLimited -> true
            is Unknown -> {
                println(code.value)
                return null
            }
        }
    }
}
