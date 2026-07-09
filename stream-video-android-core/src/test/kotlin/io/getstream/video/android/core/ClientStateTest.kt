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

import io.getstream.android.core.api.model.connection.StreamConnectedUser
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.video.android.model.User
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Direct unit test for [ClientState.handleStreamState] — core's [StreamConnectionState] is the
 * SDK's public connection state (same pattern as the Feeds SDK), so every reported state must be
 * published on [ClientState.connection] unchanged.
 *
 * The test constructs a bare [ClientState] against a mocked [StreamVideoClient] to avoid pulling
 * in the full client dependency graph.
 */
internal class ClientStateTest {

    @Test
    fun `initial connection state is Idle`() {
        val state = clientState()

        assertEquals(StreamConnectionState.Idle, state.connection.value)
    }

    @Test
    fun `handleStreamState publishes every reported state unchanged`() {
        val state = clientState()
        val reportedStates = listOf(
            StreamConnectionState.Idle,
            StreamConnectionState.Connecting.Opening("user-1"),
            StreamConnectionState.Connecting.Authenticating("user-1"),
            StreamConnectionState.Connected(connectedUserFixture(), "connection-id"),
            StreamConnectionState.Disconnected(cause = null),
            StreamConnectionState.Disconnected(cause = RuntimeException("boom")),
        )

        reportedStates.forEach { reported ->
            state.handleStreamState(reported)

            assertSame(reported, state.connection.value)
        }
    }

    private fun clientState(): ClientState {
        val streamVideo = mockk<StreamVideoClient>(relaxed = true)
        every { streamVideo.user } returns User(id = "user-1")
        return ClientState(streamVideo)
    }

    private fun connectedUserFixture(): StreamConnectedUser = StreamConnectedUser(
        createdAt = Date(0),
        id = "user-1",
        language = "en",
        role = "user",
        updatedAt = Date(0),
        blockedUserIds = emptyList(),
        teams = emptyList(),
    )
}
