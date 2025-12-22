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

package io.getstream.android.video.generated.models

/**
 * Marker class for representing delayed or "slow" call-related events.
 *
 * A [LocalEvent] mirrors real-time call events (e.g., CallRejectedEvent),
 * but is used when the original event is detected late (e.g., due to
 * push notification delivery or network disruption).
 *
 * SDK components can observe these events to replay the same instructions
 * as their real-time counterparts, or to apply special handling.
 */
abstract class LocalEvent : WSCallEvent, VideoEvent()

/**
 * Delayed counterpart of [io.getstream.android.video.generated.models.CallRejectedEvent].
 *
 * Emitted when a call rejection is inferred or detected late,
 * allowing the SDK to handle it as if a [io.getstream.android.video.generated.models.CallRejectedEvent] had
 * been received in real time (or to apply adjusted logic if needed).
 */
data class LocalCallMissedEvent(val createdById: String, val callCid: String) : LocalEvent() {
    override fun getCallCID(): String {
        return callCid
    }

    override fun getEventType(): String {
        return "local.call.reject"
    }
}
