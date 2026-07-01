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

package io.getstream.video.android.core.socket.coordinator.v2

import io.getstream.android.video.generated.models.APIError
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.ConnectionErrorEvent
import io.getstream.android.video.generated.models.CustomVideoEvent
import io.getstream.android.video.generated.models.HealthCheckEvent
import io.getstream.android.video.generated.models.OwnUserResponse
import io.getstream.android.video.generated.models.UserResponse
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Round-trip coverage for [VideoEventSerialization].
 *
 * The event set is chosen to exercise four distinct [VideoEvent] subclasses whose Moshi schema
 * is self-contained (no nested `CallResponse` graph that would require a 15-level fixture).
 * Together they validate the adapter's ability to route arbitrary product events through the
 * existing [MoshiVideoParser].
 */
internal class VideoEventSerializationTest {

    private val subject = VideoEventSerialization(MoshiVideoParser())

    @Test
    fun `round-trip ConnectedEvent`() {
        val event = ConnectedEvent(
            connectionId = "conn-1",
            createdAt = FIXED_TIME,
            me = ownUserResponseFixture(),
            type = "connection.ok",
        )

        val json = subject.serialize(event).getOrThrow()
        assertTrue(json.isNotBlank())
        assertTrue(json.contains("connection.ok"))

        val decoded = subject.deserialize(json).getOrThrow()
        assertTrue(decoded is ConnectedEvent)
        assertEquals("conn-1", (decoded as ConnectedEvent).connectionId)
    }

    @Test
    fun `round-trip CustomVideoEvent`() {
        val event = CustomVideoEvent(
            callCid = "default:call-custom",
            createdAt = FIXED_TIME,
            custom = mapOf("k" to "v"),
            user = userResponseFixture(),
            type = "custom",
        )

        val json = subject.serialize(event).getOrThrow()
        val decoded = subject.deserialize(json).getOrThrow()

        assertTrue(decoded is CustomVideoEvent)
        assertEquals("default:call-custom", (decoded as CustomVideoEvent).callCid)
    }

    @Test
    fun `round-trip ConnectionErrorEvent`() {
        val event = ConnectionErrorEvent(
            connectionId = "conn-err",
            createdAt = FIXED_TIME,
            error = APIError(
                statusCode = 500,
                code = 1,
                duration = "10ms",
                details = emptyList(),
                message = "boom",
                moreInfo = "",
                exceptionFields = emptyMap(),
                unrecoverable = false,
            ),
            type = "connection.error",
        )

        val json = subject.serialize(event).getOrThrow()
        val decoded = subject.deserialize(json).getOrThrow()

        assertTrue(decoded is ConnectionErrorEvent)
        assertEquals("conn-err", (decoded as ConnectionErrorEvent).connectionId)
    }

    @Test
    fun `round-trip HealthCheckEvent`() {
        val event = HealthCheckEvent(
            connectionId = "conn-health",
            createdAt = FIXED_TIME,
            custom = emptyMap(),
            type = "health.check",
        )

        val json = subject.serialize(event).getOrThrow()
        val decoded = subject.deserialize(json).getOrThrow()

        assertTrue(decoded is HealthCheckEvent)
        assertEquals("conn-health", (decoded as HealthCheckEvent).connectionId)
    }

    @Test
    fun `deserialize malformed JSON fails gracefully`() {
        val result = subject.deserialize("not json")

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    private fun ownUserResponseFixture(): OwnUserResponse = OwnUserResponse(
        createdAt = FIXED_TIME,
        id = "user-1",
        language = "en",
        role = "user",
        updatedAt = FIXED_TIME,
    )

    private fun userResponseFixture(): UserResponse = UserResponse(
        createdAt = FIXED_TIME,
        id = "user-2",
        language = "en",
        role = "user",
        updatedAt = FIXED_TIME,
    )

    private companion object {
        val FIXED_TIME: OffsetDateTime =
            OffsetDateTime.parse("2026-01-01T00:00:00Z")
    }
}
