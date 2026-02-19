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

package io.getstream.video.android.core.socket.common

import io.getstream.video.android.core.socket.common.token.RepositoryTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryTokenProviderTest {
    private val tokenRepository: TokenRepository = mockk()
    private val provider = RepositoryTokenProvider(tokenRepository)

    @Test
    fun `loadToken should return token from repository`() = runTest {
        val expectedToken = "jwt_123"
        every { tokenRepository.getToken() } returns expectedToken
        val result = provider.loadToken()
        assertEquals(expectedToken, result)
    }
}
