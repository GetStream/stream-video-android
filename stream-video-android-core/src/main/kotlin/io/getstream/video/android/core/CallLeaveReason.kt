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

import io.getstream.video.android.core.internal.InternalStreamVideoApi

@InternalStreamVideoApi
sealed interface CallLeaveReason {

    val message: String?

    /** The local user explicitly chose to leave. */
    @InternalStreamVideoApi
    data class UserAction(
        val cause: UserActionCause,
        override val message: String? = null,
    ) : CallLeaveReason

    /** The backend ended or rejected the call (CallEndedEvent, SFU termination, etc.). */
    @InternalStreamVideoApi
    data class Backend(
        val cause: BackendCause,
        override val message: String?,
    ) : CallLeaveReason

    /** All reconnect attempts were exhausted after a network or SFU failure. */
    @InternalStreamVideoApi
    data class RetryExhausted(
        val retryCount: Int,
        val failureCode: String?,
        override val message: String?,
    ) : CallLeaveReason

    /** A platform/system event (task removal, hold, wearable, interceptor, etc.) caused the leave. */
    @InternalStreamVideoApi
    data class SdkDriven(
        val cause: SdkCause,
        override val message: String? = null,
    ) : CallLeaveReason

    /** SDK consumer supplied an arbitrary reason. */
    @InternalStreamVideoApi
    data class Custom(
        override val message: String? = null,
    ) : CallLeaveReason
}

@InternalStreamVideoApi
enum class SdkCause {
    /** App was swiped from the recents screen. */
    TASK_REMOVED,

    /** Telecom system put the call on hold for another call. */
    CALL_ON_HOLD,

    /** SDK-level cleanup (e.g. logout, StreamVideo instance teardown). */
    CLIENT_CLEANUP,

    /** Outgoing call auto-cancel timeout elapsed with no answer. */
    RING_TIMEOUT,

    ACCEPTED_ON_OTHER_DEVICE,
    LOCAL_CALL_MISSED_EVENT,
    REJECTED_BY_ALL,
    END_CALL,
    PIP_ERROR,
    PIP_STOPPED,
    ACTIVITY_DESTROYED,
    STREAM_CALL_ACTIVITY_EXCEPTION,
    LEAVE_PREVIOUS_CALL_TO_ACCEPT_NEW_CALL,
}

@InternalStreamVideoApi
enum class UserActionCause {
    /** User rejected the call from a paired wearable device. */
    WEARABLE_REJECTED,
    WEARABLE_CANCEL,

    /** A [io.getstream.video.android.core.CallJoinInterceptor] aborted the join sequence. */
    CALL_JOIN_ABORT,
    REJECTED_BY_SELF,
    CANCELLED_BY_SELF,
    LEAVE_FROM_NOTIFICATION,
}

@InternalStreamVideoApi
enum class BackendCause {
    LEAVE_TIMEOUT_AFTER_DISCONNECT,
    SFU_DISCONNECT,
    CALL_ENDED_EVENT,
    CALL_ENDED_SFU_EVENT,
    RING_FAILED,
}
