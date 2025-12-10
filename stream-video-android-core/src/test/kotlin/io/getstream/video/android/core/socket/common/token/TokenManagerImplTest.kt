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
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenManagerImplTest {
    private lateinit var tokenRepository: TokenRepository
    private lateinit var tokenManager: TokenManagerImpl
    private lateinit var provider: PersistingTokenProvider

    @Before
    fun setup() {
        tokenRepository = mockk(relaxed = true)
        provider = mockk()
        tokenManager = TokenManagerImpl(tokenRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ---------------------------------------------------------
    // updateToken
    // ---------------------------------------------------------
    @Test
    fun `updateToken should update repository`() {
        val token = "ABC"

        tokenManager.updateToken(token)

        verify { tokenRepository.updateToken(token) }
    }

    // ---------------------------------------------------------
    // getToken + hasToken
    // ---------------------------------------------------------
    @Test
    fun `getToken should return token from repository`() {
        every { tokenRepository.getToken() } returns "JWT123"

        val result = tokenManager.getToken()

        assertEquals("JWT123", result)
    }

    @Test
    fun `hasToken should return true when token is not EMPTY_TOKEN`() {
        every { tokenRepository.getToken() } returns "VALID"

        assertTrue(tokenManager.hasToken())
    }

    @Test
    fun `hasToken should return false when token is EMPTY_TOKEN`() {
        every { tokenRepository.getToken() } returns TokenManagerImpl.EMPTY_TOKEN

        assertFalse(tokenManager.hasToken())
    }

    // ---------------------------------------------------------
    // expireToken
    // ---------------------------------------------------------
    @Test
    fun `expireToken should update repository with EMPTY_TOKEN`() {
        tokenManager.expireToken()

        verify { tokenRepository.updateToken(TokenManagerImpl.EMPTY_TOKEN) }
    }

    // ---------------------------------------------------------
    // setTokenProvider + hasTokenProvider
    // ---------------------------------------------------------
    @Test
    fun `hasTokenProvider returns false before provider is set`() {
        assertFalse(tokenManager.hasTokenProvider())
    }

    @Test
    fun `hasTokenProvider returns true after provider is set`() {
        tokenManager.setTokenProvider(mockk<PersistingTokenProvider>())

        assertTrue(tokenManager.hasTokenProvider())
    }

    // ---------------------------------------------------------
    // loadSync
    // ---------------------------------------------------------
    @Test
    fun `loadSync should load token from provider and persist it`() = runTest {
        val expectedToken = "fresh_token"
        coEvery { provider.loadToken() } returns expectedToken
        tokenManager.setTokenProvider(provider)

        val result = tokenManager.loadSync()

        assertEquals(expectedToken, result)
        coVerify { provider.loadToken() }
        verify { tokenRepository.updateToken(expectedToken) }
    }

    // ---------------------------------------------------------
    // ensureTokenLoaded
    // ---------------------------------------------------------
    @Test
    fun `ensureTokenLoaded loads token when no token exists`() = runTest {
        // No token present
        every { tokenRepository.getToken() } returns TokenManagerImpl.EMPTY_TOKEN

        coEvery { provider.loadToken() } returns "new_token"
        tokenManager.setTokenProvider(provider)

        tokenManager.ensureTokenLoaded()

        coVerify { provider.loadToken() }
        verify { tokenRepository.updateToken("new_token") }
    }

    @Test
    fun `ensureTokenLoaded does nothing when token already exists`() = runTest {
        every { tokenRepository.getToken() } returns "already_present"

        tokenManager.setTokenProvider(provider)

        tokenManager.ensureTokenLoaded()

        coVerify(exactly = 0) { provider.loadToken() }
    }
}
