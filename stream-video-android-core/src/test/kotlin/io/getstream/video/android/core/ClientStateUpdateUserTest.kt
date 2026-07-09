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
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

internal class ClientStateUpdateUserTest {

    @Test
    fun `updateUser publishes the new user to the state flow`() {
        // ClientState casts its client to StreamVideoClient internally.
        val state = ClientState(mockk<StreamVideoClient>(relaxed = true))
        val user = User(id = "guest-1", name = "Guest", type = UserType.Guest)

        state.updateUser(user)

        assertEquals(user, state.user.value)
    }
}
