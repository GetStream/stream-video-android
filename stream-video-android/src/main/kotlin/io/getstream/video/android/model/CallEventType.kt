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

import io.getstream.video.android.model.CallEventType.ACCEPTED
import io.getstream.video.android.model.CallEventType.CANCELLED
import io.getstream.video.android.model.CallEventType.REJECTED
import io.getstream.video.android.model.CallEventType.UNDEFINED
import stream.video.coordinator.client_v1_rpc.UserEventType
import stream.video.coordinator.client_v1_rpc.UserEventType.USER_EVENT_TYPE_ACCEPTED_CALL
import stream.video.coordinator.client_v1_rpc.UserEventType.USER_EVENT_TYPE_CANCELLED_CALL
import stream.video.coordinator.client_v1_rpc.UserEventType.USER_EVENT_TYPE_REJECTED_CALL
import stream.video.coordinator.client_v1_rpc.UserEventType.USER_EVENT_TYPE_UNSPECIFIED

public enum class CallEventType {
    ACCEPTED, REJECTED, CANCELLED, UNDEFINED
}

public fun UserEventType.toCallEventType(): CallEventType = when (this) {
    USER_EVENT_TYPE_ACCEPTED_CALL -> ACCEPTED
    USER_EVENT_TYPE_REJECTED_CALL -> REJECTED
    USER_EVENT_TYPE_CANCELLED_CALL -> CANCELLED
    USER_EVENT_TYPE_UNSPECIFIED -> UNDEFINED
}

public fun CallEventType.toUserEventType(): UserEventType = when (this) {
    ACCEPTED -> USER_EVENT_TYPE_ACCEPTED_CALL
    REJECTED -> USER_EVENT_TYPE_REJECTED_CALL
    CANCELLED -> USER_EVENT_TYPE_CANCELLED_CALL
    UNDEFINED -> USER_EVENT_TYPE_UNSPECIFIED
}
