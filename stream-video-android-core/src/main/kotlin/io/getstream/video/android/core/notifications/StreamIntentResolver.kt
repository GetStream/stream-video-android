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

package io.getstream.video.android.core.notifications

import android.app.PendingIntent
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_ONGOING_CALL
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.model.NotificationType
import io.getstream.video.android.model.StreamCallId

/**
 * We'll deprecate this [StreamIntentResolver] soon
 * Use the [StreamIntentResolverWithPayload]
 */
interface StreamIntentResolver : StreamIntentResolverWithPayload {
    /**
     * Search for an activity that can receive incoming calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "searchIncomingCallPendingIntent(callId, notificationId, payload)",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun searchIncomingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = callId.hashCode(),
    ): PendingIntent? {
        return searchIncomingCallPendingIntent(callId, notificationId, emptyMap())
    }

    /**
     * Search for an activity that is used for outgoing calls.
     * Calls are considered outgoing until the call is accepted.
     *
     * @param callId the call id
     * @param notificationId the notification ID.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "searchOutgoingCallPendingIntent(callId, notificationId, payload)",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun searchOutgoingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = callId.hashCode(),
    ): PendingIntent? {
        return searchIncomingCallPendingIntent(callId, notificationId, emptyMap())
    }

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "searchNotificationCallPendingIntent(callId, notificationId, payload)",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun searchNotificationCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? {
        return searchNotificationCallPendingIntent(callId, notificationId, emptyMap())
    }

    /**
     * Search for an activity that can receive missed calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("searchMissedCallPendingIntent(callId, notificationId, payload)"),
        level = DeprecationLevel.WARNING,
    )
    fun searchMissedCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? {
        return searchMissedCallPendingIntent(callId, notificationId, emptyMap())
    }

    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("getDefaultPendingIntent(payload)"),
        level = DeprecationLevel.WARNING,
    )
    fun getDefaultPendingIntent(): PendingIntent {
        return getDefaultPendingIntent(emptyMap())
    }

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("searchLiveCallPendingIntent(callId, notificationId, payload)"),
        level = DeprecationLevel.WARNING,
    )
    fun searchLiveCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
    ): PendingIntent? {
        return searchLiveCallPendingIntent(callId, notificationId, emptyMap())
    }

    /**
     * Search for an activity that can accept call from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     * @return The [PendingIntent] which can trigger a component to consume accept call events.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("searchAcceptCallPendingIntent(callId, notificationId, payload)"),
        level = DeprecationLevel.WARNING,
    )
    fun searchAcceptCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = callId.getNotificationId(NotificationType.Incoming),
    ): PendingIntent? {
        return searchAcceptCallPendingIntent(callId, notificationId, emptyMap())
    }

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("searchRejectCallPendingIntent(callId, payload)"),
        level = DeprecationLevel.WARNING,
    )
    fun searchRejectCallPendingIntent(
        callId: StreamCallId,
    ): PendingIntent? {
        return searchRejectCallPendingIntent(callId, emptyMap())
    }

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith("searchEndCallPendingIntent(callId, payload)"),
        level = DeprecationLevel.WARNING,
    )
    fun searchEndCallPendingIntent(
        callId: StreamCallId,
    ): PendingIntent? {
        return searchEndCallPendingIntent(callId, emptyMap())
    }

    /**
     * Searches an activity that will accept the [ACTION_ONGOING_CALL] intent and jump right back into the call.
     *
     * @param callId the call id
     * @param notificationId the notification ID.
     */
    @Deprecated(
        "Use the one with payload: Map<String, Any?>",
        replaceWith = ReplaceWith(
            "searchOngoingCallPendingIntent(callId, notificationId, payload)",
        ),
        level = DeprecationLevel.WARNING,
    )
    fun searchOngoingCallPendingIntent(callId: StreamCallId, notificationId: Int): PendingIntent? {
        return searchOngoingCallPendingIntent(callId, notificationId, emptyMap())
    }
}
