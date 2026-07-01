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

import io.getstream.android.core.api.model.value.StreamUserId
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class IntegrationStreamTokenProviderTest {

    @Test
    fun `loadToken delegates to wrapped TokenProvider`() = runTest {
        val delegate = mockk<TokenProvider>()
        coEvery { delegate.loadToken() } returns "fake-jwt"
        val subject = IntegrationStreamTokenProvider(delegate)

        val token = subject.loadToken(StreamUserId.fromString("any"))

        coVerify(exactly = 1) { delegate.loadToken() }
        assertEquals("fake-jwt", token.rawValue)
    }

    @Test
    fun `userId parameter is unused across invocations`() = runTest {
        val delegate = mockk<TokenProvider>()
        coEvery { delegate.loadToken() } returns "fake-jwt"
        val subject = IntegrationStreamTokenProvider(delegate)

        subject.loadToken(StreamUserId.fromString("user-a"))
        subject.loadToken(StreamUserId.fromString("user-b"))

        // Two invocations produce two identical delegate calls regardless of userId.
        coVerify(exactly = 2) { delegate.loadToken() }
    }
}
