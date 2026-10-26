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

package io.getstream.video.android.core.socket.common.token

import io.getstream.android.core.api.authentication.StreamTokenProvider
import io.getstream.android.core.api.model.value.StreamToken
import io.getstream.android.core.api.model.value.StreamUserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamTokenLoaderTest {

    @Test
    fun `loadToken asks the shared provider for the current user`() = runTest {
        val provider = mockk<StreamTokenProvider>()
        coEvery { provider.loadToken(any()) } returns StreamToken.fromString("jwt")
        var currentUserId = "user-1"
        val loader = StreamTokenLoader(provider) { currentUserId }

        assertEquals("jwt", loader.loadToken())
        currentUserId = "guest-2"
        assertEquals("jwt", loader.loadToken())

        coVerify(exactly = 1) { provider.loadToken(StreamUserId.fromString("user-1")) }
        coVerify(exactly = 1) { provider.loadToken(StreamUserId.fromString("guest-2")) }
    }
}
