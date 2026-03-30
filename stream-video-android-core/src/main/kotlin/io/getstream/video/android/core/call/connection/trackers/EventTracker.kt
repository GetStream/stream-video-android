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

package io.getstream.video.android.core.call.connection.trackers

import io.getstream.video.android.core.internal.InternalStreamVideoApi
import kotlinx.coroutines.flow.MutableStateFlow

@InternalStreamVideoApi
enum class Tracker {
    CALL_ACCEPTED_START,
    CALL_ACCEPTED_END,
    CALL_JOIN_START,
    CALL_JOIN_END,
    PUBLISHER_CONNECTED,
    SUBSCRIBER_CONNECTED,
    FIRST_INBOUND_RTP,
    RINGING_STATE_TIMER_START,
    RINGING_STATE_TIMER_FINISH,
}

@InternalStreamVideoApi
data class CollectedEvent(
    val tracker: Tracker,
    val time: Long? = null,
)

@InternalStreamVideoApi
public class EventTracker {

    val acceptedStartEvent = MutableStateFlow(CollectedEvent(Tracker.CALL_ACCEPTED_START))
    val acceptedEndEvent = MutableStateFlow(CollectedEvent(Tracker.CALL_ACCEPTED_START))

    val joinStartEvent = MutableStateFlow(CollectedEvent(Tracker.CALL_JOIN_START))
    val joinEndEvent = MutableStateFlow(CollectedEvent(Tracker.CALL_JOIN_END))
    val publisherConnectedEvent = MutableStateFlow(CollectedEvent(Tracker.PUBLISHER_CONNECTED))
    val subscriberConnectedEvent = MutableStateFlow(CollectedEvent(Tracker.SUBSCRIBER_CONNECTED))
    val firstInboundRtpArrived = MutableStateFlow(CollectedEvent(Tracker.FIRST_INBOUND_RTP))
    val ringingStateTimerStarted =
        MutableStateFlow(CollectedEvent(Tracker.RINGING_STATE_TIMER_START))
    val ringingStateTimerFinished =
        MutableStateFlow(CollectedEvent(Tracker.RINGING_STATE_TIMER_FINISH))
}
