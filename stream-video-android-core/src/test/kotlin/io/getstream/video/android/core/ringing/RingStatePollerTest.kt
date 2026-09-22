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

package io.getstream.video.android.core.ringing

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.GetCallRingStateResponse
import io.getstream.result.Error
import io.getstream.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class RingStatePollerTest {

    private val sessionId = "session-1"
    private val ringTimeout = 30_000L

    private fun ringState() = GetCallRingStateResponse(
        callCid = "default:call-1",
        createdByUserId = "caller",
        duration = "0ms",
        sessionId = sessionId,
    )

    /**
     * Builds a poller whose clock is the test scheduler's virtual time, so `advanceTimeBy` moves
     * both the delays and the quiet-period arithmetic together.
     */
    private fun TestScope.poller(
        config: RingStatePollingConfig? = RingStatePollingConfig(),
        fetch: suspend (
            String,
        ) -> Result<GetCallRingStateResponse> = { Result.Success(ringState()) },
        onRingState: suspend (GetCallRingStateResponse) -> Unit = {},
    ) = RingStatePoller(
        scope = this,
        config = config,
        now = { testScheduler.currentTime },
        fetch = fetch,
        onRingState = onRingState,
    )

    @Test
    fun `does not poll while the ring is younger than the quiet period`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(14_999)

        assertThat(reads).isEqualTo(0)
        poller.stop()
    }

    @Test
    fun `polls on the interval once the ring has gone quiet`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(15_001)
        assertThat(reads).isEqualTo(1)

        advanceTimeBy(5_000)
        assertThat(reads).isEqualTo(2)

        poller.stop()
    }

    @Test
    fun `a ring event restarts the quiet period instead of stopping the poller`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(10_000)

        // A rejection from one callee does not settle a group ring, so the poller stays armed —
        // but the socket just proved it is alive, so the wait starts over.
        poller.onRingParticipantStatusUpdate()
        advanceTimeBy(10_000)
        assertThat(reads).isEqualTo(0)

        advanceTimeBy(5_001)
        assertThat(reads).isEqualTo(1)

        poller.stop()
    }

    @Test
    fun `stops at the ring deadline rather than racing the local auto-drop`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(60_000)

        // 15s quiet period, then reads at 20/25/30s — the read that would land past the
        // deadline is not issued.
        assertThat(reads).isEqualTo(3)
        poller.stop()
    }

    @Test
    fun `keeps polling after a failed read`() = runTest {
        var reads = 0
        val poller = poller(
            fetch = {
                reads++
                Result.Failure(Error.GenericError("offline"))
            },
        )

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(25_001)

        assertThat(reads).isEqualTo(3)
        poller.stop()
    }

    @Test
    fun `stop halts polling`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(15_001)
        assertThat(reads).isEqualTo(1)

        poller.stop()
        advanceTimeBy(30_000)
        assertThat(reads).isEqualTo(1)
    }

    @Test
    fun `a null config disables polling entirely`() = runTest {
        var reads = 0
        val poller = poller(config = null, fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(60_000)

        assertThat(reads).isEqualTo(0)
    }

    @Test
    fun `does not start without a session id or a ring timeout`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ "" }, { ringTimeout })
        poller.start({ sessionId }, { 0 })
        advanceTimeBy(60_000)

        assertThat(reads).isEqualTo(0)
    }

    @Test
    fun `caps the polling window when the ring settings allow a long ring`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        // An app may set a ten minute auto-cancel. Polling for its whole duration would be one
        // read every 5s per ringing caller against a rate limited endpoint.
        poller.start({ sessionId }, { 600_000L })
        advanceTimeBy(600_000)

        // 60s ceiling: quiet period to 15s, then reads at 20..55s.
        assertThat(reads).isEqualTo(9)
        poller.stop()
    }

    /**
     * The poller is no longer stopped when the accept is first seen, so that a join which then
     * fails is retried by the next read. The ring deadline is fixed when polling starts and is
     * never extended, so staying alive cannot turn into an unbounded read loop.
     */
    @Test
    fun `an accept that keeps being reported does not extend the ring window`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(600_000)

        // 30s window: quiet period to 15s, then reads at 15, 20 and 25s, and no further.
        assertThat(reads).isEqualTo(3)
        poller.stop()
    }

    @Test
    fun `a timing that would spin the read loop is rejected at construction`() {
        // Disabling polling is done by passing a null config, never by zeroing a timing, so a
        // non-positive interval is a mistake rather than an intent to switch it off.
        assertFailsWith<IllegalArgumentException> { RingStatePollingConfig(intervalMs = 0) }
        assertFailsWith<IllegalArgumentException> {
            RingStatePollingConfig(defaultRingWindowMs = 0)
        }
        assertFailsWith<IllegalArgumentException> { RingStatePollingConfig(startAfterMs = -1) }
    }

    @Test
    fun `starting an already running poller is ignored`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { ringTimeout })
        // A second arm must not add a parallel loop, or every ring event that re-enters the
        // ringing state would double the read rate.
        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(600_000)

        // The reads of one loop over a 30s window, not two loops interleaved.
        assertThat(reads).isEqualTo(3)
        poller.stop()
    }

    @Test
    fun `a ring window of zero falls back to the default rather than never polling`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        // 0 means "no auto-drop" in the ring settings, not "a zero length ring".
        poller.start({ sessionId }, { 0L })
        advanceTimeBy(600_000)

        assertThat(reads).isEqualTo(3)
        poller.stop()
    }

    @Test
    fun `falls back to a default ring window when the ring settings are not known`() = runTest {
        var reads = 0
        val poller = poller(fetch = {
            reads++
            Result.Success(ringState())
        })

        poller.start({ sessionId }, { null })
        advanceTimeBy(600_000)

        // 30s default, not the 60s ceiling: quiet period to 15s, then reads at 15, 20 and 25s.
        assertThat(reads).isEqualTo(3)
        poller.stop()
    }

    @Test
    fun `stops on a status that cannot start succeeding`() = runTest {
        var reads = 0
        val poller = poller(
            fetch = {
                reads++
                // The session is unknown or belongs to another call; retrying only spends the
                // app's rate limit.
                Result.Failure(
                    Error.NetworkError("not found", serverErrorCode = 16, statusCode = 404),
                )
            },
        )

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(30_000)

        assertThat(reads).isEqualTo(1)
    }

    @Test
    fun `waits for a session id and keeps the first one it sees`() = runTest {
        // An outgoing ring can reach the poller before its session lands, and once the call ends
        // the call has no current session at all — so the poller latches the first one.
        var current: String? = null
        val seen = mutableListOf<String>()
        val poller = poller(
            fetch = { id ->
                seen.add(id)
                Result.Success(ringState())
            },
        )

        // A longer window than the default: this exercises three polls, which does not fit in one
        // 30s ring.
        poller.start({ current }, { 60_000L })
        advanceTimeBy(20_001)
        assertThat(seen).isEmpty()

        current = sessionId
        advanceTimeBy(5_000)
        assertThat(seen).containsExactly(sessionId)

        current = null
        advanceTimeBy(5_000)
        assertThat(seen).containsExactly(sessionId, sessionId)

        poller.stop()
    }

    @Test
    fun `hands every successful read to the reconciler`() = runTest {
        val seen = mutableListOf<GetCallRingStateResponse>()
        val poller = poller(onRingState = { seen.add(it) })

        poller.start({ sessionId }, { ringTimeout })
        advanceTimeBy(20_001)

        assertThat(seen).hasSize(2)
        assertThat(seen.first().sessionId).isEqualTo(sessionId)
        poller.stop()
    }
}
