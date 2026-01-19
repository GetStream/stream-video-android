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

package io.getstream.video.android.core.token

import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.utils.positiveRandomInt
import io.getstream.video.android.core.utils.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CacheableTokenProviderTest {

    private val delegatedTokenProvider: TokenProvider = mockk(relaxed = true)
    private val cacheableTokenProvider = CacheableTokenProvider(delegatedTokenProvider)

    @Test
    fun `Initial cached token should be empty`() {
        assertEquals("", cacheableTokenProvider.getCachedToken())
    }

    @Test
    fun `CacheableTokenProvider should store last value`() = runTest {
        val tokens = List(positiveRandomInt(20)) { randomString() }
        coEvery { delegatedTokenProvider.loadToken() } returns tokens.random()

        val result = mutableListOf<String>()
        repeat(3) {
            val newToken = cacheableTokenProvider.loadToken()
            result.add(newToken)
        }

        assertEquals(result.size, 3)
        coVerify(exactly = 3) { delegatedTokenProvider.loadToken() }
        assertEquals(result.last(), cacheableTokenProvider.getCachedToken())
    }

    @Test
    fun `CacheableTokenProvider should delegate the process to obtain a token to his delegated token provider`() = runTest {
        val tokens = List(positiveRandomInt(20)) { randomString() }
        coEvery { delegatedTokenProvider.loadToken() } returns tokens.random()

        val result = mutableListOf<String>()
        repeat(3) {
            val newToken = cacheableTokenProvider.loadToken()
            result.add(newToken)
        }

        assertEquals(result.size, 3)
        coVerify(exactly = 3) { delegatedTokenProvider.loadToken() }
    }
}
