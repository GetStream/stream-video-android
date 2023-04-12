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

package io.getstream.video.android.core

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import java.time.Instant
import java.util.*

@RunWith(RobolectricTestRunner::class)
class ParticipantStateTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private val logger by taggedLogger("Test:ParticipantStateTest")

    @Test
    fun `start recording a call`() = runTest {
        val call = client.call("default", randomUUID())

        // when we get the information about a call there are some details about the participant
        val result = call.get()
        assertSuccess(result)
        result.onSuccess {
            logger.d { it.toString() }
        }

        // when we receive this event there is more info about the participant
        val now = Instant.now()
        val participant = Participant(
            user_id = "test",
            session_id = "123",
            joined_at = now,
            track_lookup_prefix = "hello",
            connection_quality = ConnectionQuality.CONNECTION_QUALITY_GOOD,
            is_speaking = false,
            is_dominant_speaker = false,
            audio_level = 10F,
            name = "Thierry",
            image = "missing"
        )
        val event = ParticipantJoinedEvent(participant = participant, call.cid)
        clientImpl.fireEvent(event)
    }
}
