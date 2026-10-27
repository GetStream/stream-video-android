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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.sorting.participant
import io.getstream.video.android.core.sorting.stubCallActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import stream.video.sfu.models.Participant

class ParticipantStateJoinedAtTest {

    private val scope = CoroutineScope(StandardTestDispatcher(TestCoroutineScheduler()))

    @Test
    fun `joinedAt keeps the instant the SFU sent`() {
        val state = participant("session", scope, stubCallActions())

        state.updateFromParticipantInfo(
            Participant(
                session_id = "session",
                joined_at = java.time.Instant.parse("2026-10-22T08:15:30.123456789Z"),
            ),
        )

        assertThat(state.joinedAt.value)
            .isEqualTo(OffsetDateTime.of(2026, 10, 22, 8, 15, 30, 123_456_789, ZoneOffset.UTC))
    }

    @Test
    fun `joinedAt falls back to now when the SFU sends none`() {
        val state = participant("session", scope, stubCallActions())
        val before = OffsetDateTime.now(ZoneOffset.UTC)

        state.updateFromParticipantInfo(Participant(session_id = "session"))

        val joinedAt = state.joinedAt.value!!
        assertThat(joinedAt).isAtLeast(before)
        assertThat(joinedAt).isAtMost(OffsetDateTime.now(ZoneOffset.UTC))
    }
}
