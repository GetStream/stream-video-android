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

package io.getstream.video.android.core.user

import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only access to the SDK's current user.
 *
 * The repository is the single source of truth for the SDK user: every component
 * that needs to read the current identity (socket auth payload, call state,
 * notification routing, etc.) takes a [UserRepository] so it can never drift
 * from the canonical value.
 *
 * Kept `internal` — the SDK's public surface for the current user is still
 * `StreamVideo.user` and `ClientState.user`.
 */
internal interface UserRepository {
    /** The current user. */
    val user: User

    /** Hot flow of the current user — emits on every [WritableUserRepository.setUser]. */
    val userFlow: StateFlow<User>
}

/**
 * Write side of [UserRepository]. Only the SDK owns a reference to this sub-interface;
 * everyone else holds a [UserRepository] so we can't accidentally mutate user state
 * from a reader and then have the rest of the SDK keep stale data.
 */
internal interface WritableUserRepository : UserRepository {
    fun setUser(user: User)
}

/**
 * Default in-memory implementation. The user is kept in a [MutableStateFlow] so
 * observers (e.g. `ClientState.user`) update automatically when [setUser] is called.
 */
internal class StreamUserRepositoryImpl(initial: User) : WritableUserRepository {
    private val _userFlow: MutableStateFlow<User> = MutableStateFlow(initial)
    override val userFlow: StateFlow<User> = _userFlow
    override val user: User get() = _userFlow.value
    override fun setUser(user: User) {
        _userFlow.value = user
    }
}
