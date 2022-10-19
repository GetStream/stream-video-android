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
import io.getstream.video.android.model.StreamCallCid as CallCid
import io.getstream.video.android.model.StreamCallId as CallId
import io.getstream.video.android.model.StreamCallType as CallType

public sealed interface StreamCallState : java.io.Serializable {

    public object Idle : StreamCallState

    public interface Active : StreamCallState {
        public val callGuid: CallGuid
    }

    public data class Starting(
        override val callGuid: CallGuid,
        val memberUserIds: List<String>,
        val ringing: Boolean
    ) : Active

    public data class Outgoing(
        override val callGuid: CallGuid,
        val createdByUserId: String,
        val broadcastingEnabled: Boolean,
        val recordingEnabled: Boolean,
        val users: Map<String, CallUser>,
        val members: Map<String, CallMember>,
        val acceptedByCallee: Boolean
    ) : Active

    public data class Incoming(
        override val callGuid: CallGuid,
        val createdByUserId: String,
        val broadcastingEnabled: Boolean,
        val recordingEnabled: Boolean,
        val users: Map<String, CallUser>,
        val members: Map<String, CallMember>,
        val acceptedByMe: Boolean
    ) : Active

    public data class Joining(
        override val callGuid: CallGuid,
        val createdByUserId: String,
        val broadcastingEnabled: Boolean,
        val recordingEnabled: Boolean,
        val users: Map<String, CallUser>,
        val members: Map<String, CallMember>
    ) : Active

    public interface InCall : Active {
        public val createdByUserId: String
        public val broadcastingEnabled: Boolean
        public val recordingEnabled: Boolean
        public val users: Map<String, CallUser>
        public val members: Map<String, CallMember>
        public val callUrl: String
        public val userToken: String
        public val iceServers: List<IceServer>
    }

    public data class Connected(
        override val callGuid: CallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>,
        override val callUrl: String,
        override val userToken: String,
        override val iceServers: List<IceServer>
    ) : InCall

    public data class Connecting(
        override val callGuid: CallGuid,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val users: Map<String, CallUser>,
        override val members: Map<String, CallMember>,
        override val callUrl: String,
        override val userToken: String,
        override val iceServers: List<IceServer>
    ) : InCall

    public data class Drop(
        override val callGuid: CallGuid,
        val reason: DropReason
    ) : Active
}

public sealed class DropReason {
    public data class Timeout(val waitMillis: Long) : DropReason()
    public data class Failure(val error: VideoError) : DropReason()
    public object Rejected : DropReason()
    public object Cancelled : DropReason()
}

public data class CallGuid(
    val type: CallType,
    val id: CallId,
    val cid: CallCid
)
