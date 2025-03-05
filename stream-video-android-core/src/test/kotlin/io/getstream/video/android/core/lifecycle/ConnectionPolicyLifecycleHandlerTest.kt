/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.lifecycle

import io.getstream.video.android.core.socket.common.SocketConnectionPolicy
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketStateService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ConnectionPolicyLifecycleHandlerTest {

    private lateinit var connectionPolicy1: SocketConnectionPolicy
    private lateinit var connectionPolicy2: SocketConnectionPolicy
    private lateinit var socketStateService: CoordinatorSocketStateService
    private lateinit var lifecycleHandler: ConnectionPolicyLifecycleHandler

    @Before
    fun setup() {
        connectionPolicy1 = mockk()
        connectionPolicy2 = mockk()
        socketStateService = mockk(relaxed = true)
        lifecycleHandler = ConnectionPolicyLifecycleHandler(
            connectionPolicies = listOf(connectionPolicy1, connectionPolicy2),
            socketStateService = socketStateService,
        )
    }

    @Test
    fun `resume should call onResume when all policies allow connection`() = runTest {
        // Given
        every { connectionPolicy1.shouldConnect() } returns true
        every { connectionPolicy2.shouldConnect() } returns true

        // When
        lifecycleHandler.resume()

        // Then
        coVerify(exactly = 1) { socketStateService.onResume() }
    }

    @Test
    fun `resume should not call onResume when any policy prevents connection`() = runTest {
        // Given
        every { connectionPolicy1.shouldConnect() } returns true
        every { connectionPolicy2.shouldConnect() } returns false

        // When
        lifecycleHandler.resume()

        // Then
        coVerify(exactly = 0) { socketStateService.onResume() }
    }

    @Test
    fun `resume should not call onResume when all policies prevent connection`() = runTest {
        // Given
        every { connectionPolicy1.shouldConnect() } returns false
        every { connectionPolicy2.shouldConnect() } returns false

        // When
        lifecycleHandler.resume()

        // Then
        coVerify(exactly = 0) { socketStateService.onResume() }
    }

    @Test
    fun `stopped should call onStop when all policies allow disconnection`() = runTest {
        // Given
        every { connectionPolicy1.shouldDisconnect() } returns true
        every { connectionPolicy2.shouldDisconnect() } returns true

        // When
        lifecycleHandler.stopped()

        // Then
        coVerify(exactly = 1) { socketStateService.onStop() }
    }

    @Test
    fun `stopped should not call onStop when any policy prevents disconnection`() = runTest {
        // Given
        every { connectionPolicy1.shouldDisconnect() } returns true
        every { connectionPolicy2.shouldDisconnect() } returns false

        // When
        lifecycleHandler.stopped()

        // Then
        coVerify(exactly = 0) { socketStateService.onStop() }
    }

    @Test
    fun `stopped should not call onStop when all policies prevent disconnection`() = runTest {
        // Given
        every { connectionPolicy1.shouldDisconnect() } returns false
        every { connectionPolicy2.shouldDisconnect() } returns false

        // When
        lifecycleHandler.stopped()

        // Then
        coVerify(exactly = 0) { socketStateService.onStop() }
    }
}
