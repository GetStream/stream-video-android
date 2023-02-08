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

package io.getstream.video.android.core.user

import io.getstream.video.android.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Used to expose a mechanism that allows the SDK to fetch users. These users can then be invited
 * to calls, explored in isolation and more.
 */
public interface UsersProvider {

    /**
     * Provides a single instance of a user list that can be presented on the UI.
     */
    public fun provideUsers(): List<User>

    /**
     * Provides a state backed list of users that can change over time.
     */
    public val userState: StateFlow<List<User>>
}

/**
 * Default empty implementation of [UsersProvider]. Useful if you don't want to provide any users in
 * your UI or call flow.
 */
public object EmptyUsersProvider : UsersProvider {
    override val userState: StateFlow<List<User>> = MutableStateFlow(provideUsers())

    override fun provideUsers(): List<User> = emptyList()
}
