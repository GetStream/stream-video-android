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

import io.getstream.result.Error
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.android.video.generated.models.VideoEvent
import stream.video.sfu.models.WebsocketReconnectStrategy

public sealed class StreamWebSocketEvent {
    data class Error(
        val streamError: io.getstream.result.Error,
        val reconnectStrategy: WebsocketReconnectStrategy? = null,
    ) : StreamWebSocketEvent()
    data class VideoMessage(val videoEvent: VideoEvent) : StreamWebSocketEvent()
    data class SfuMessage(val sfuEvent: SfuDataEvent) : StreamWebSocketEvent()
}
