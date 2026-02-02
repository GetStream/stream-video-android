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

package io.getstream.video.android.core

import androidx.compose.runtime.Stable
import io.getstream.video.android.core.call.RtcSession

@Stable
public sealed interface RealtimeConnection {
    /**
     * We start out in the PreJoin state. This is before call.join is called
     */
    public data object PreJoin : RealtimeConnection

    /**
     * Join is in progress
     */
    public data object InProgress : RealtimeConnection

    /**
     * We set the state to Joined as soon as the call state is available
     */
    public data class Joined(val session: RtcSession) :
        RealtimeConnection // joined, participant state is available, you can render the call. Video isn't ready yet

    /**
     * True when the peer connections are ready
     */
    public data object Connected :
        RealtimeConnection // connected to RTC, able to receive and send video

    /**
     * Reconnecting is true whenever Rtc isn't available and trying to recover
     * If the subscriber peer connection breaks we'll reconnect
     * If the publisher peer connection breaks we'll reconnect
     * Also if the network provider from the OS says that internet is down we'll set it to reconnecting
     */
    public data object Reconnecting :
        RealtimeConnection // reconnecting to recover from temporary issues

    public data object Migrating : RealtimeConnection
    public data class Failed(val error: Any) : RealtimeConnection // permanent failure
    public data object Disconnected : RealtimeConnection // normal disconnect by the app
}
