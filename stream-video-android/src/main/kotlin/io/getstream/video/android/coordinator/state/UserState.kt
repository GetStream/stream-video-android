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

package io.getstream.video.android.coordinator.state

import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class UserState {

    /**
     * Represents the internal Flow that caches the last user instance and emits it to all
     * subscribers.
     */
    private val userFlow = MutableStateFlow(EMPTY_USER)
    public val user: StateFlow<User> = userFlow

    /**
     * Emits the new user update to all listeners.
     *
     * @param [user] The new user instance to set.
     */
    public fun setUser(user: User) {
        this.userFlow.value = user
    }

    public companion object {
        private val EMPTY_USER = User("", "", "", "", "", emptyList(), emptyMap())
    }
}
