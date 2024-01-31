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

package io.getstream.video.android.core.socket

import org.openapitools.client.models.VideoEvent

public sealed class SocketState {
    /** We haven't started to connect yet */
    object NotConnected : SocketState() {
        override fun toString(): String = "Not Connected"
    }

    /** Connection is in progress */
    object Connecting : SocketState() {
        override fun toString(): String = "Connecting"
    }

    /** We are connected, the most common state */
    data class Connected(val event: VideoEvent) : SocketState()

    /** There is no internet available */
    object NetworkDisconnected : SocketState() {
        override fun toString(): String = "NetworkDisconnected"
    }

    /** A temporary error broken the connection, socket will retry */
    data class DisconnectedTemporarily(val error: Throwable) : SocketState()

    /** A permanent error broken the connection, socket will not retry */
    data class DisconnectedPermanently(val error: Throwable) : SocketState()

    /** You called socket.disconnect(), socket is disconnected */
    object DisconnectedByRequest : SocketState() {
        override fun toString(): String = "DisconnectedByRequest"
    }
}
