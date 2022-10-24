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

package io.getstream.video.android.model.state

import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.model.CallMember
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.IceServer
import java.io.Serializable
import io.getstream.video.android.model.StreamCallCid as CallCid
import io.getstream.video.android.model.StreamCallId as CallId
import io.getstream.video.android.model.StreamCallType as CallType

public sealed interface StreamCallState : Serializable {

    public object Idle : StreamCallState { override fun toString(): String = "Idle" }

    public sealed class Active : StreamCallState {
        public abstract val callGuid: StreamCallGuid
    }

    public sealed interface Joinable : StreamCallState {
        public val callGuid: StreamCallGuid
    }

    public data class Starting(
        override val callGuid: StreamCallGuid,
        val memberUserIds: List<String>,
        val ringing: Boolean
    ) : Active(), Joinable

    public sealed class Started : Active() {
        public abstract val createdByUserId: String
        public abstract val broadcastingEnabled: Boolean
        public abstract val recordingEnabled: Boolean
        public abstract val users: Map<String, CallUser>
        public abstract val members: Map<String, CallMember>
    }

    public data class Outgoing(
        override val callGuid: StreamCallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>,
        val acceptedByCallee: Boolean
    ) : Started(), Joinable

    public data class Incoming(
        override val callGuid: StreamCallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>,
        val acceptedByMe: Boolean
    ) : Started(), Joinable

    public data class Joining(
        override val callGuid: StreamCallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>
    ) : Started()

    public sealed class InCall : Started() {
        public abstract val callUrl: String
        public abstract val userToken: String
        public abstract val iceServers: List<IceServer>
    }

    public data class Connected(
        override val callGuid: StreamCallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>,
        override val callUrl: String,
        override val userToken: String,
        override val iceServers: List<IceServer>
    ) : InCall()

    public data class Connecting(
        override val callGuid: StreamCallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>,
        override val callUrl: String,
        override val userToken: String,
        override val iceServers: List<IceServer>
    ) : InCall()

    public data class Drop(
        override val callGuid: StreamCallGuid,
        val reason: DropReason,
    ) : Active()
}

public sealed class DropReason : Serializable {
    public data class Timeout(val waitMillis: Long) : DropReason()
    public data class Failure(val error: VideoError) : DropReason()
    public data class Rejected(val byUserId: String) : DropReason()
    public data class Cancelled(val byUserId: String) : DropReason()
    public object Ended : DropReason() { override fun toString(): String = "Ended" }
}

public data class StreamCallGuid(
    val type: CallType,
    val id: CallId,
    val cid: CallCid
) : Serializable
