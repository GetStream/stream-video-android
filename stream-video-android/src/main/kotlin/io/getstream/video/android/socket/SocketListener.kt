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

package io.getstream.video.android.socket

import io.getstream.video.android.errors.DisconnectCause
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.utils.VideoError

public open class SocketListener {

    public open fun onConnecting() {
    }

    public open fun onConnected(event: ConnectedEvent) {
    }

    public open fun onDisconnected(cause: DisconnectCause) {
    }

    public open fun onError(error: VideoError) {
    }

    public open fun onEvent(event: VideoEvent) {
    }
}
