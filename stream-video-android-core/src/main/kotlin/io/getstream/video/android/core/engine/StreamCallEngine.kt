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

package io.getstream.video.android.core.engine

import io.getstream.result.Error
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.model.CallEventType
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.JoinedCall
import io.getstream.video.android.core.model.state.StreamCallState
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.VideoEvent
import stream.video.sfu.event.JoinRequest

public interface StreamCallEngine {

    /**
     * Represents the state of the currently active call.
     */
    public val callState: StateFlow<StreamCallState>

    /**
     * Called when [VideoEvent] received from Coordinator.
     */
    public fun onCoordinatorEvent(event: VideoEvent)

    /**
     * Called when [SfuDataEvent] received from SFU.
     */
    public fun onSfuEvent(event: SfuDataEvent)

    /**
     * Called when [JoinRequest] message is sent to SFU.
     */
    public fun onSfuJoinSent(request: JoinRequest)

    /**
     * Called when one of the following actions happens:
     * - User joined a created Meeting.
     * - User joined a received Meeting.
     * - Caller joined a Call which has been accepted by Callee.
     * - Callee joined an accepted incoming Call.
     */
    public fun onCallJoined(joinedCall: JoinedCall)

    /**
     * Called when one of the following actions happens:
     * - User starts a Meeting.
     * - User receives a Meeting.
     * - Caller dials specified [participantIds].
     * - Callee accepts an incoming Call.
     */
    public fun onCallStarting(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean,
        forcedNewCall: Boolean
    )

    /**
     * Called when Caller started a Call and waits it to be accepted by a Callee.
     */
    public fun onCallStarted(call: CallMetadata)

    /**
     * Called when one of the following actions happens:
     * - User joins a created Meeting.
     * - User joins a received Meeting.
     * - Caller joins a Call which has been accepted by Callee.
     * - Callee joins an accepted incoming Call.
     */
    public fun onCallJoining(call: CallMetadata)

    /**
     * Called when a Call has been failed.
     */
    public fun onCallFailed(error: Error)

    /**
     * Called when [CallEventType] message is about to be sent to Coordinator.
     */
    public fun onCallEventSending(callCid: String, eventType: CallEventType)

    /**
     * Called when [CallEventType] message is sent to Coordinator.
     */
    public fun onCallEventSent(callCid: String, eventType: CallEventType)
}
