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

import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import org.junit.Test
import kotlin.test.assertEquals

internal class BindableWritableUserRepositoryTest {

    private val guest = User(id = "guest-1", type = UserType.Guest)

    @Test
    fun `setUser after bind delivers immediately`() {
        val repo = BindableWritableUserRepository()
        val delivered = mutableListOf<User>()
        repo.bind(delivered::add)

        repo.setUser(guest)

        assertEquals(listOf(guest), delivered)
    }

    @Test
    fun `setUser before bind is buffered and delivered on bind`() {
        val repo = BindableWritableUserRepository()
        val delivered = mutableListOf<User>()

        repo.setUser(guest)
        repo.bind(delivered::add)

        assertEquals(listOf(guest), delivered)
    }

    @Test
    fun `only the latest pre-bind user is delivered`() {
        val repo = BindableWritableUserRepository()
        val delivered = mutableListOf<User>()
        val stale = guest.copy(name = "stale")
        val fresh = guest.copy(name = "fresh")

        repo.setUser(stale)
        repo.setUser(fresh)
        repo.bind(delivered::add)

        assertEquals(listOf(fresh), delivered)
    }
}
