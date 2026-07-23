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

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.RtcSession
import io.mockk.mockk
import org.junit.Test

/**
 * Unit tests for [CallSessionManager], the single source of truth for the live RTC
 * session and the bookkeeping shared across the join / reconnect flows.
 */
class CallSessionManagerTest {

    private val call = mockk<Call>(relaxed = true)

    private fun manager() = CallSessionManager(call)

    @Test
    fun `session starts empty and can be replaced`() {
        val manager = manager()
        assertThat(manager.session.value).isNull()

        val session = mockk<RtcSession>(relaxed = true)
        manager.session.value = session

        assertThat(manager.session.value).isSameInstanceAs(session)
    }

    @Test
    fun `session and unified session ids are non-blank uuids and distinct`() {
        val manager = manager()

        assertThat(manager.sessionId).isNotEmpty()
        assertThat(manager.unifiedSessionId).isNotEmpty()
        assertThat(manager.sessionId).isNotEqualTo(manager.unifiedSessionId)
    }

    @Test
    fun `each manager gets its own session ids`() {
        assertThat(manager().sessionId).isNotEqualTo(manager().sessionId)
        assertThat(manager().unifiedSessionId).isNotEqualTo(manager().unifiedSessionId)
    }

    @Test
    fun `sessionId is mutable`() {
        val manager = manager()
        manager.sessionId = "custom-session-id"
        assertThat(manager.sessionId).isEqualTo("custom-session-id")
    }

    @Test
    fun `location defaults to null and is mutable`() {
        val manager = manager()
        assertThat(manager.location).isNull()

        manager.location = "amsterdam"
        assertThat(manager.location).isEqualTo("amsterdam")
    }

    @Test
    fun `reconnect and timing counters default to zero and are mutable`() {
        val manager = manager()
        assertThat(manager.nonFastReconnectAttempts).isEqualTo(0)
        assertThat(manager.connectStartTime).isEqualTo(0L)
        assertThat(manager.reconnectStartTime).isEqualTo(0L)

        manager.nonFastReconnectAttempts += 3
        manager.connectStartTime = 111L
        manager.reconnectStartTime = 222L

        assertThat(manager.nonFastReconnectAttempts).isEqualTo(3)
        assertThat(manager.connectStartTime).isEqualTo(111L)
        assertThat(manager.reconnectStartTime).isEqualTo(222L)
    }
}
