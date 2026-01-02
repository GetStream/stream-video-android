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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import stream.video.sfu.models.PeerType

/**
 * The type of peer connections, either a [PUBLISHER] that sends data to the call or a [SUBSCRIBER]
 * that receives and decodes the data from the server.
 */
@Stable
public enum class StreamPeerType {
    PUBLISHER,
    SUBSCRIBER,
}

public fun StreamPeerType.toPeerType(): PeerType = when (this) {
    StreamPeerType.PUBLISHER -> PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED
    StreamPeerType.SUBSCRIBER -> PeerType.PEER_TYPE_SUBSCRIBER
}
