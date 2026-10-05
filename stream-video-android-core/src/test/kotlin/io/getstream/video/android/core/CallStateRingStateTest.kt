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
import io.getstream.android.video.generated.models.GetCallRingStateResponse
import io.getstream.video.android.core.base.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

/**
 * Covers how a polled ring state is folded into call state. The join that may follow is not
 * asserted here — it needs a live coordinator — so these pin the inputs that decision reads.
 */
@RunWith(RobolectricTestRunner::class)
class CallStateRingStateTest : IntegrationTestBase(connectCoordinatorWS = false) {

    private fun ringState(
        acceptedBy: Map<String, OffsetDateTime> = emptyMap(),
        rejectedBy: Map<String, OffsetDateTime> = emptyMap(),
        missedBy: Map<String, OffsetDateTime> = emptyMap(),
        sessionEndedAt: OffsetDateTime? = null,
        callEndedAt: OffsetDateTime? = null,
    ) = GetCallRingStateResponse(
        callCid = "default:ring-state",
        createdByUserId = "caller",
        duration = "0ms",
        sessionId = "session-1",
        acceptedBy = acceptedBy,
        rejectedBy = rejectedBy,
        missedBy = missedBy,
        sessionEndedAt = sessionEndedAt,
        callEndedAt = callEndedAt,
    )

    // A fixed instant on a plain UTC offset: ThreeTenBP has no time-zone data registered in
    // unit tests, so anything resolving a zone id throws.
    private val now: OffsetDateTime =
        OffsetDateTime.of(2026, 9, 15, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun `a polled accept lands in acceptedBy`() = runTest {
        val call = client.call("default", randomUUID())

        call.state.updateFromRingState(ringState(acceptedBy = mapOf("callee" to now)))

        assertThat(call.state.acceptedBy.value).containsExactly("callee")
    }

    @Test
    fun `a polled reject lands in rejectedBy`() = runTest {
        val call = client.call("default", randomUUID())

        call.state.updateFromRingState(ringState(rejectedBy = mapOf("callee" to now)))

        assertThat(call.state.rejectedBy.value).containsExactly("callee")
    }

    /**
     * The failure mode AND-1413 describes: merging one map while replacing another drops
     * rejections. Polling every few seconds would otherwise erase them repeatedly.
     */
    @Test
    fun `polling one map does not erase the other`() = runTest {
        val call = client.call("default", randomUUID())

        call.state.updateFromRingState(ringState(rejectedBy = mapOf("first" to now)))
        call.state.updateFromRingState(ringState(acceptedBy = mapOf("second" to now)))

        assertThat(call.state.rejectedBy.value).containsExactly("first")
        assertThat(call.state.acceptedBy.value).containsExactly("second")
    }

    @Test
    fun `an empty read does not narrow what is already known`() = runTest {
        val call = client.call("default", randomUUID())

        call.state.updateFromRingState(ringState(acceptedBy = mapOf("callee" to now)))
        call.state.updateFromRingState(ringState())

        assertThat(call.state.acceptedBy.value).containsExactly("callee")
    }

    @Test
    fun `an ended session is recorded even when the same read carries an accept`() = runTest {
        val call = client.call("default", randomUUID())

        call.state.updateFromRingState(
            ringState(acceptedBy = mapOf("callee" to now), sessionEndedAt = now),
        )

        // endedAt is what updateRingingState reads to refuse the join, so it must be set by the
        // same read that reports the accept, not by a later one.
        assertThat(call.state.endedAt.value).isNotNull()
    }

    @Test
    fun `a call end is recorded`() = runTest {
        val call = client.call("default", randomUUID())

        call.state.updateFromRingState(ringState(callEndedAt = now))

        assertThat(call.state.endedAt.value).isNotNull()
    }

    @Test
    fun `an already known end is not overwritten by a later read`() = runTest {
        val call = client.call("default", randomUUID())
        val earlier = now.minusSeconds(30)

        call.state.updateFromRingState(ringState(callEndedAt = earlier))
        call.state.updateFromRingState(ringState(callEndedAt = now))

        assertThat(call.state.endedAt.value).isEqualTo(earlier)
    }
}
