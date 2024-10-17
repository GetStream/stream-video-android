package io.getstream.video.android.core.socket.common

import io.getstream.video.android.model.SfuToken
import io.getstream.video.android.model.User
import stream.video.sfu.event.JoinRequest

public sealed class ConnectionConf {
    var isReconnection: Boolean = false
        private set
    abstract val endpoint: String
    abstract val apiKey: String
    abstract val user: User

    data class AnonymousConnectionConf(
        override val endpoint: String,
        override val apiKey: String,
        override val user: User,
    ) : ConnectionConf()

    data class UserConnectionConf(
        override val endpoint: String,
        override val apiKey: String,
        override val user: User,
    ) : ConnectionConf()

    data class SfuConnectionConf(
        override val endpoint: String,
        override val apiKey: String,
        override val user: User = User.anonymous(),
        val joinRequest: JoinRequest,
        val token: SfuToken,
    ) : ConnectionConf()

    internal fun asReconnectionConf(): ConnectionConf = this.also { isReconnection = true }

    internal val id: String
        get() = when (this) {
            is AnonymousConnectionConf -> "!anon"
            else -> user.id
        }
}