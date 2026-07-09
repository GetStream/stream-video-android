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
import io.getstream.video.android.core.socket.coordinator.v2.toVideoConnectionState
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Table-driven mapping test for [StreamConnectionState] -> [ConnectionState] extension.
 *
 * Asserts the temporary placeholder mapping. A follow-up PR reshapes the video
 * [ConnectionState] sealed interface to mirror core 1:1; both this test and the mapper body
 * flip together in that PR.
 */
internal class ClientStateConnectionMappingTest {

    @Test
    fun `Idle maps to PreConnect`() {
        val result = (StreamConnectionState.Idle as StreamConnectionState).toVideoConnectionState()
        assertEquals(ConnectionState.PreConnect, result)
        // TODO: update expected value when the ConnectionState sealed interface is rewritten
    }

    @Test
    fun `Connecting Opening maps to Loading`() {
        val result: ConnectionState =
            StreamConnectionState.Connecting.Opening("user-1").toVideoConnectionState()
        assertEquals(ConnectionState.Loading, result)
        // TODO: update expected value when the ConnectionState sealed interface is rewritten
    }

    @Test
    fun `Connecting Authenticating maps to Loading`() {
        val result: ConnectionState =
            StreamConnectionState.Connecting.Authenticating("user-1").toVideoConnectionState()
        assertEquals(ConnectionState.Loading, result)
        // TODO: update expected value when the ConnectionState sealed interface is rewritten
    }

    @Test
    fun `Connected maps to Connected`() {
        val result: ConnectionState = StreamConnectionState
            .Connected(connectedUserFixture(), "connection-id")
            .toVideoConnectionState()
        assertEquals(ConnectionState.Connected, result)
        // TODO: update expected value when the ConnectionState sealed interface is rewritten
    }

    @Test
    fun `Disconnected with null cause maps to Disconnected`() {
        val result: ConnectionState =
            StreamConnectionState.Disconnected(cause = null).toVideoConnectionState()
        assertEquals(ConnectionState.Disconnected, result)
        // TODO: update expected value when the ConnectionState sealed interface is rewritten
    }

    @Test
    fun `Disconnected with cause maps to Failed`() {
        val boom = IllegalStateException("boom")
        val result: ConnectionState =
            StreamConnectionState.Disconnected(cause = boom).toVideoConnectionState()

        assertTrue(result is ConnectionState.Failed)
        assertEquals("boom", result.error.message)
        // TODO: update expected value when the ConnectionState sealed interface is rewritten
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
