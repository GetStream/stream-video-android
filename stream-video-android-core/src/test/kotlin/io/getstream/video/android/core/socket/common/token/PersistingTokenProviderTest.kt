/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.socket.common.token

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistingTokenProviderTest {
    private val tokenProvider: TokenProvider = mockk()
    private val tokenRepository: TokenRepository = mockk(relaxed = true)

    private val provider = PersistingTokenProvider(tokenProvider, tokenRepository)

    @Test
    fun `loadToken should return token from provider and persist it`() = runTest {
        // Given
        val expectedToken = "jwt_123"
        coEvery { tokenProvider.loadToken() } returns expectedToken

        // When
        val result = provider.loadToken()

        // Then
        assertEquals(expectedToken, result)

        // Verify internal behavior:
        coVerify(exactly = 1) { tokenProvider.loadToken() }
        coVerify(exactly = 1) { tokenRepository.updateToken(expectedToken) }
    }
}
