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

package io.getstream.video.android.core.model

import org.webrtc.PeerConnection.PeerConnectionState

/**
 * Represents [io.getstream.video.android.call.connection.StreamPeerConnection] connection state.
 */
public enum class StreamPeerConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    FAILED,
    DISCONNECTED,
    CLOSED;
}

@JvmSynthetic
internal fun PeerConnectionState.toDomainPeerConnectionState(): StreamPeerConnectionState = when (this) {
    PeerConnectionState.NEW -> StreamPeerConnectionState.NEW
    PeerConnectionState.CONNECTING -> StreamPeerConnectionState.CONNECTING
    PeerConnectionState.CONNECTED -> StreamPeerConnectionState.CONNECTED
    PeerConnectionState.FAILED -> StreamPeerConnectionState.FAILED
    PeerConnectionState.DISCONNECTED -> StreamPeerConnectionState.DISCONNECTED
    PeerConnectionState.CLOSED -> StreamPeerConnectionState.CLOSED
}
