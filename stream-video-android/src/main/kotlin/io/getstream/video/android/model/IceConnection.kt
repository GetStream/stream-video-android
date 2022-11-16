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

package io.getstream.video.android.model

import org.webrtc.PeerConnection.IceConnectionState

public enum class IceConnection {
    NEW,
    CHECKING,
    CONNECTED,
    COMPLETED,
    FAILED,
    DISCONNECTED,
    CLOSED;
}

internal fun IceConnectionState.toIceConnection(): IceConnection = when (this) {
    IceConnectionState.NEW -> IceConnection.NEW
    IceConnectionState.CHECKING -> IceConnection.CHECKING
    IceConnectionState.CONNECTED -> IceConnection.CONNECTED
    IceConnectionState.COMPLETED -> IceConnection.COMPLETED
    IceConnectionState.FAILED -> IceConnection.FAILED
    IceConnectionState.DISCONNECTED -> IceConnection.DISCONNECTED
    IceConnectionState.CLOSED -> IceConnection.CLOSED
}
