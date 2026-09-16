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
import io.getstream.video.android.core.base.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The join source is a local-only value handed from the ring to the join lifecycle that follows.
 * Reading it clears it, so it can only ever be attributed to the join it was set for — a later
 * reconnect opening its own lifecycle must not inherit it.
 */
@RunWith(RobolectricTestRunner::class)
class RingJoinSourceTest : IntegrationTestBase(connectCoordinatorWS = false) {

    @Test
    fun `wire values match the reporting contract`() {
        assertThat(RingJoinSource.WebSocket.value).isEqualTo("ring-ws")
        assertThat(RingJoinSource.PollApi.value).isEqualTo("ring-poll-api")
    }

    @Test
    fun `a set source is handed to the next join`() = runTest {
        val call = client.call("default", randomUUID())

        call.setJoinSource(RingJoinSource.PollApi)

        assertThat(call.consumeJoinSource()).isEqualTo(RingJoinSource.PollApi)
    }

    @Test
    fun `reading the source clears it, so a later join does not inherit it`() = runTest {
        val call = client.call("default", randomUUID())

        call.setJoinSource(RingJoinSource.WebSocket)
        call.consumeJoinSource()

        assertThat(call.consumeJoinSource()).isNull()
    }

    @Test
    fun `a join with no ring behind it has no source`() = runTest {
        val call = client.call("default", randomUUID())

        assertThat(call.consumeJoinSource()).isNull()
    }

    @Test
    fun `the latest source wins when a ring is re-attributed before joining`() = runTest {
        val call = client.call("default", randomUUID())

        call.setJoinSource(RingJoinSource.WebSocket)
        call.setJoinSource(RingJoinSource.PollApi)

        assertThat(call.consumeJoinSource()).isEqualTo(RingJoinSource.PollApi)
    }
}
