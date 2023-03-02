/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.app.user

import io.getstream.video.android.app.utils.getUsers
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UsersProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUsersProvider(private val currentUserId: String) : UsersProvider {

    override fun provideUsers(): List<User> {
        return mockUsers()
    }

    private fun mockUsers(): List<User> {
        return getUsers().filter { it.id != currentUserId }
    }

    override val userState: StateFlow<List<User>> = MutableStateFlow(provideUsers())
}
