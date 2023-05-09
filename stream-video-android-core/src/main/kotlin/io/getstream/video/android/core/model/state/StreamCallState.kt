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

package io.getstream.video.android.core.model.state

import io.getstream.result.Error
import io.getstream.video.android.core.model.CallDetails
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.SfuToken
import io.getstream.video.android.core.model.StreamCallGuid
import io.getstream.video.android.core.model.StreamCallKind
import io.getstream.video.android.core.model.StreamSfuSessionId
import io.getstream.video.android.core.model.User
import java.io.Serializable
import java.util.Date

/**
 * Represents possible state of [StreamCallEngine].
 */
public sealed interface StreamCallState : Serializable {

    /**
     * Represents a state which can be joined.
     */
    public sealed interface Joinable : StreamCallState

    /**
     * Signifies there is no active call.
     */
    public object Idle : StreamCallState, Joinable {
        override fun toString(): String = "Idle"
    }

    /**
     * Represents an active state.
     */
    public sealed class Active : StreamCallState {
        public abstract val callGuid: StreamCallGuid
        public abstract val callKind: StreamCallKind
    }

    public sealed class Started : Active() {
        public abstract val createdByUserId: String
        public abstract val broadcastingEnabled: Boolean
        public abstract val recordingEnabled: Boolean
        public abstract val createdAt: StreamDate
        public abstract val updatedAt: StreamDate
        public abstract val users: Map<String, CallUser>
        public abstract val callDetails: CallDetails

        //        public abstract val callEgress: CallEgress
        public abstract val custom: Map<String, Any>
    }

    /**
     * Signifies that the caller starts the call.
     */
    public data class Outgoing(
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val createdAt: StreamDate,
        override val updatedAt: StreamDate,
        override val users: Map<String, CallUser>,
        override val callDetails: CallDetails,
//        override val callEgress: CallEgress,
        override val custom: Map<String, Any>,
        val rejections: List<User>,
        val acceptedByCallee: Boolean
    ) : Started(), Joinable

    /**
     * Signifies that the callee received an incoming call.
     */
    public data class Incoming(
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val createdAt: StreamDate,
        override val updatedAt: StreamDate,
        override val users: Map<String, CallUser>,
        override val callDetails: CallDetails,
//        override val callEgress: CallEgress,
        override val custom: Map<String, Any>,
        val acceptedByMe: Boolean,
    ) : Started(), Joinable

    public data class Joining(
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val createdAt: StreamDate,
        override val updatedAt: StreamDate,
        override val users: Map<String, CallUser>,
        override val callDetails: CallDetails,
//        override val callEgress: CallEgress,
        override val custom: Map<String, Any>
    ) : Started()

    public sealed class InCall : Started() {
        public abstract val callUrl: String
        public abstract val sfuToken: SfuToken
        public abstract val iceServers: List<IceServer>
    }

    /**
     * Set when joined to Coordinator.
     * @see [io.getstream.video.android.core.engine.StreamCallEngine.onCallJoined]
     */
    public data class Joined(
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val createdAt: StreamDate,
        override val updatedAt: StreamDate,
        override val users: Map<String, CallUser>,
        override val callUrl: String,
        override val sfuToken: SfuToken,
        override val iceServers: List<IceServer>,
        override val callDetails: CallDetails,
//        override val callEgress: CallEgress,
        override val custom: Map<String, Any>
    ) : InCall()

    /**
     * Set when SFU join request is sent.
     * @see [stream.video.sfu.event.JoinRequest]
     * @see [io.getstream.video.android.core.engine.StreamCallEngine.onSfuJoinSent]
     */
    public data class Connecting(
        val sfuSessionId: StreamSfuSessionId,
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val createdAt: StreamDate,
        override val updatedAt: StreamDate,
        override val users: Map<String, CallUser>,
        override val callUrl: String,
        override val sfuToken: SfuToken,
        override val iceServers: List<IceServer>,
        override val callDetails: CallDetails,
//        override val callEgress: CallEgress,
        override val custom: Map<String, Any>
    ) : InCall()

    /**
     * Set when SFU join response is received.
     * @see [io.getstream.video.android.core.events.JoinCallResponseEvent]
     * @see [io.getstream.video.android.core.engine.StreamCallEngine.onSfuEvent]
     */
    public data class Connected(
        val sfuSessionId: StreamSfuSessionId,
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        override val createdByUserId: String,
        override val broadcastingEnabled: Boolean,
        override val recordingEnabled: Boolean,
        override val createdAt: StreamDate,
        override val updatedAt: StreamDate,
        override val users: Map<String, CallUser>,
        override val callUrl: String,
        override val sfuToken: SfuToken,
        override val iceServers: List<IceServer>,
        override val callDetails: CallDetails,
//        override val callEgress: CallEgress,
        override val custom: Map<String, Any>
    ) : InCall()

    /**
     * Signifies that the call was dropped because of the following [reason].
     */
    public data class Drop(
        override val callGuid: StreamCallGuid,
        override val callKind: StreamCallKind,
        val reason: DropReason,
    ) : Active()
}

/**
 * Represents the call drop reason.
 */
public sealed class DropReason : Serializable {
    public data class Timeout(val waitMillis: Long) : DropReason()
    public data class Failure(val error: Error) : DropReason()
    public data class Rejected(val byUserId: String) : DropReason()
    public data class Cancelled(val byUserId: String) : DropReason()
    public object Ended : DropReason() {
        override fun toString(): String = "Ended"
    }
}

public sealed class StreamDate : Serializable {
    public data class Specified(val date: Date) : StreamDate() {
        public constructor(ts: Long) : this(Date(ts))
    }

    public object Undefined : StreamDate() {
        override fun toString(): String = "Undefined"
    }

    public companion object {
        public fun from(ts: Long): StreamDate = when (ts) {
            0L, -1L -> Undefined
            else -> Specified(ts)
        }

        public fun from(date: Date?): StreamDate = when (date) {
            null -> Undefined
            else -> Specified(date)
        }
    }
}

internal fun StreamCallState.Started.copy(
    createdByUserId: String = this.createdByUserId,
    broadcastingEnabled: Boolean = this.broadcastingEnabled,
    recordingEnabled: Boolean = this.recordingEnabled,
    createdAt: StreamDate = this.createdAt,
    updatedAt: StreamDate = this.updatedAt,
    users: Map<String, CallUser> = this.users,
): StreamCallState = when (this) {
    is StreamCallState.Joined -> copy(
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        users = users,
    )

    is StreamCallState.Connecting -> copy(
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        users = users,
    )

    is StreamCallState.Connected -> copy(
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        users = users,
    )

    is StreamCallState.Incoming -> copy(
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        users = users,
    )

    is StreamCallState.Joining -> copy(
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        users = users,
    )

    is StreamCallState.Outgoing -> copy(
        createdByUserId = createdByUserId,
        broadcastingEnabled = broadcastingEnabled,
        recordingEnabled = recordingEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
        users = users,
    )
}
