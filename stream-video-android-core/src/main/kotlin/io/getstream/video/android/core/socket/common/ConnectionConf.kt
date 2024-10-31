/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
