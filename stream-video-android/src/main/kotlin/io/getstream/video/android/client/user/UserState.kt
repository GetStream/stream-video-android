package io.getstream.video.android.client.user

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import stream.video.User

public class UserState {

    /**
     * Represents the internal Flow that caches the last user instance and emits it to all
     * subscribers.
     */
    private val userFlow = MutableStateFlow(EMPTY_USER)
    public val user: Flow<User> = userFlow

    /**
     * Emits the new user update to all listeners.
     *
     * @param [user] The new user instance to set.
     */
    public fun setUser(user: User) {
        this.userFlow.value = user
    }

    public companion object {
        private val EMPTY_USER = User()
    }
}