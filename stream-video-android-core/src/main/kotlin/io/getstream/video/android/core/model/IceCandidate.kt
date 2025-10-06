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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import io.getstream.webrtc.IceCandidate as RtcIceCandidate

@Stable
@kotlinx.serialization.Serializable
public data class IceCandidate(
    internal val sdpMid: String,
    internal val sdpMLineIndex: Int,
    internal val candidate: String,
    internal val usernameFragment: String?,
)

internal fun RtcIceCandidate.toDomainCandidate(): IceCandidate {
    return IceCandidate(
        sdpMid,
        sdpMLineIndex,
        sdp,
        "",
    )
}

internal fun IceCandidate.toRtcCandidate(): RtcIceCandidate {
    return io.getstream.webrtc.IceCandidate(
        sdpMid,
        sdpMLineIndex,
        candidate,
    )
}
