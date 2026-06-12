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
internal interface LeaveCause {
    val defaultMessage: String
}

@InternalStreamVideoApi
sealed interface CallLeaveReason {

    val message: String

    /** The local user explicitly chose to leave. */
    @InternalStreamVideoApi
    data class UserAction(
        val cause: UserActionCause,
        override val message: String = cause.defaultMessage,
    ) : CallLeaveReason

    /** The backend ended or rejected the call (CallEndedEvent, SFU termination, etc.). */
    @InternalStreamVideoApi
    data class Backend(
        val cause: BackendCause,
        override val message: String = cause.defaultMessage,
    ) : CallLeaveReason

    /** All reconnect attempts were exhausted after a network or SFU failure. */
    @InternalStreamVideoApi
    data class RetryExhausted(
        val retryCount: Int,
        val failureCode: String?,
        override val message: String = "Retry exhausted after $retryCount attempts (${failureCode ?: "unknown"})",
    ) : CallLeaveReason

    /** A platform/system event (task removal, hold, wearable, interceptor, etc.) caused the leave. */
    @InternalStreamVideoApi
    data class SdkDriven(
        val cause: SdkCause,
        override val message: String = cause.defaultMessage,
    ) : CallLeaveReason

    /** SDK consumer supplied an arbitrary reason. */
    @InternalStreamVideoApi
    data class Custom(
        override val message: String,
    ) : CallLeaveReason
}

@InternalStreamVideoApi
enum class SdkCause(
    override val defaultMessage: String,
) : LeaveCause {

    /** App was swiped from the recents screen. */
    TASK_REMOVED("App removed from recents"),

    /** Telecom system put the call on hold for another call. */
    CALL_ON_HOLD("Call put on hold"),

    /** SDK-level cleanup (e.g. logout, StreamVideo instance teardown). */
    CLIENT_CLEANUP("client-cleanup"),

    /** Outgoing call auto-cancel timeout elapsed with no answer. */
    RING_TIMEOUT("RingTimeout"),

    ACCEPTED_ON_OTHER_DEVICE("accepted-on-another-device"),
    LOCAL_CALL_MISSED_EVENT("LocalCallMissedEvent"),
    REJECTED_BY_ALL("All participants rejected the call"),
    END_CALL("CALL_ENDED"),
    PIP_ERROR("Error in Pip"),
    PIP_STOPPED("PIP stopped"),
    ACTIVITY_DESTROYED("Activity destroyed"),
    STREAM_CALL_ACTIVITY_EXCEPTION("STREAM_CALL_ACTIVITY_EXCEPTION"),
    LEAVE_PREVIOUS_CALL_TO_ACCEPT_NEW_CALL("Leave previous call to accept new call"),
}

@InternalStreamVideoApi
enum class UserActionCause(
    override val defaultMessage: String,
) : LeaveCause {

    /** User rejected the call from a paired wearable device. */
    WEARABLE_REJECTED("Call rejected on wearable"),

    WEARABLE_CANCEL("Canceled from wearable"),

    /** A [io.getstream.video.android.core.CallJoinInterceptor] aborted the join sequence. */
    CALL_JOIN_ABORT("Call join aborted"),

    REJECTED_BY_SELF("Rejected by self"),
    CANCELLED_BY_SELF("Cancelled the call"),
    LEAVE_FROM_NOTIFICATION("User left from notification action"),
}

@InternalStreamVideoApi
enum class BackendCause(
    override val defaultMessage: String,
) : LeaveCause {

    LEAVE_TIMEOUT_AFTER_DISCONNECT("Leave timeout after disconnect"),
    SFU_DISCONNECT("SFU:DISCONNECT"),
    CALL_ENDED_EVENT("CallEndedEvent"),
    CALL_ENDED_SFU_EVENT("CallEndedSfuEvent"),
    CALL_SESSION_ENDED_EVENT("CallSessionEndedEvent"),
    RING_FAILED("Ring failed"),
}
