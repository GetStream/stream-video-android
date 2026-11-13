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

package io.getstream.video.android.core.model

import androidx.test.core.app.ApplicationProvider
import com.jakewharton.threetenabp.AndroidThreeTen
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.android.video.generated.models.CallRequest
import io.getstream.android.video.generated.models.ConnectionErrorEvent
import io.getstream.android.video.generated.models.JoinCallRequest
import io.getstream.android.video.generated.models.NoiseCancellationSettings
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.socket.common.SocketErrorMessage
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Codegen and reflection disagree on missing fields, nulls, and defaults.
 * These cases are the ones that have to keep matching after the generated-model keep is removed.
 */
@RunWith(RobolectricTestRunner::class)
class GeneratedModelCodegenTest {

    private val moshi = Serializer.moshi

    @Before
    fun setUp() {
        AndroidThreeTen.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `missing field uses the kotlin default and an explicit null stays null`() {
        val adapter = moshi.adapter(JoinCallRequest::class.java)
        val missing = adapter.fromJson("""{"location":"us"}""")!!
        assertEquals("us", missing.location)
        assertNull(missing.create)
        assertEquals(emptyList<String>(), missing.migratingFromList)

        val explicitNull = adapter.fromJson("""{"location":"us","create":null}""")!!
        assertNull(explicitNull.create)
    }

    @Test
    fun `json name is honored and unknown fields are ignored`() {
        val parsed = moshi.adapter(JoinCallRequest::class.java).fromJson(
            """{"location":"us","hint_high_scale_livestream_publisher":true,"unused":1}""",
        )!!
        assertEquals(true, parsed.hintHighScaleLivestreamPublisher)
    }

    @Test
    fun `map of any round trips through the generated adapter`() {
        val parsed = moshi.adapter(CallRequest::class.java).fromJson(
            """{"custom":{"n":1,"s":"x"}}""",
        )!!
        assertEquals(1.0, parsed.custom?.get("n"))
        assertEquals("x", parsed.custom?.get("s"))
    }

    @Test
    fun `nested enum uses the handwritten adapter`() {
        val parsed = moshi.adapter(NoiseCancellationSettings::class.java)
            .fromJson("""{"mode":"disabled"}""")!!
        assertEquals(NoiseCancellationSettings.Mode.Disabled, parsed.mode)
    }

    @Test
    fun `video event type selects connection error and the generated adapter parses it`() {
        val adapter = moshi.adapter(VideoEvent::class.java)
        val event = adapter.fromJson(
            """
            {
              "type": "connection.error",
              "connection_id": "conn-1",
              "created_at": "2020-01-01T00:00:00Z",
              "error": {
                "code": 1,
                "duration": "0s",
                "message": "nope",
                "more_info": "",
                "StatusCode": 400
              }
            }
            """.trimIndent(),
        )
        assertTrue(event is ConnectionErrorEvent)
        val errorEvent = event as ConnectionErrorEvent
        assertEquals("conn-1", errorEvent.connectionId)
        assertEquals("connection.error", errorEvent.type)
        assertEquals(emptyList<Int>(), errorEvent.error.details)
        assertNull(errorEvent.error.unrecoverable)
        assertTrue(
            moshi.adapter(ConnectionErrorEvent::class.java).toString()
                .contains("GeneratedJsonAdapter(ConnectionErrorEvent)"),
        )
    }

    @Test
    fun `socket error parses through the generated adapter without kotlin reflect`() {
        val message = MoshiVideoParser().fromJson(
            """
            {"error":{"code":4,"message":"nope","statusCode":400,"moreInfo":"https://example","details":[{"code":1,"messages":["bad"]}]}}
            """.trimIndent(),
            SocketErrorMessage::class.java,
        )
        val error = message.error!!
        assertEquals(4, error.code)
        assertEquals("nope", error.message)
        assertEquals(400, error.statusCode)
        assertEquals("https://example", error.moreInfo)
        assertEquals("", error.duration)
        assertEquals(1, error.details.single().code)
        assertEquals(listOf("bad"), error.details.single().messages)
    }
}
