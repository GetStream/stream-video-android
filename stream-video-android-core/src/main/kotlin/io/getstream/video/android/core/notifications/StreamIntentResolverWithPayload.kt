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
import io.getstream.video.android.model.StreamCallId

interface StreamIntentResolverWithPayload {
    /**
     * Search for an activity that can receive incoming calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     * @param payload The payload from Push Notification
     */
    fun searchIncomingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Search for an activity that is used for outgoing calls.
     * Calls are considered outgoing until the call is accepted.
     *
     * @param callId the call id
     * @param notificationId the notification ID.
     * @param payload The payload from Push Notification
     */
    fun searchOutgoingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     * @param payload The payload from Push Notification
     */
    fun searchNotificationCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Search for an activity that can receive missed calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     * @param payload The payload from Push Notification
     */
    fun searchMissedCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
        payload: Map<String, Any?>,
    ): PendingIntent?

    fun getDefaultPendingIntent(payload: Map<String, Any?>): PendingIntent

    /**
     * Search for an activity that can receive live calls from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     * @param payload The payload from Push Notification
     */
    fun searchLiveCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Search for an activity that can accept call from Stream Server.
     *
     * @param callId The call id from the incoming call.
     * @param notificationId the notification ID.
     * @param payload The payload from Push Notification
     * @return The [PendingIntent] which can trigger a component to consume accept call events.
     */
    fun searchAcceptCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int = NotificationHandler.INCOMING_CALL_NOTIFICATION_ID,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @param payload The payload from Push Notification
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    fun searchRejectCallPendingIntent(
        callId: StreamCallId,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Searches for a broadcast receiver that can consume the [ACTION_REJECT_CALL] intent to reject
     * a call from the Stream Server.
     *
     * @param callId The ID of the call.
     * @param payload The payload from Push Notification
     * @return The [PendingIntent] which can trigger a component to consume the call rejection event.
     */
    fun searchEndCallPendingIntent(
        callId: StreamCallId,
        payload: Map<String, Any?>,
    ): PendingIntent?

    /**
     * Searches an activity that will accept the [ACTION_ONGOING_CALL] intent and jump right back into the call.
     *
     * @param callId the call id
     * @param payload The payload from Push Notification
     * @param notificationId the notification ID.
     */
    fun searchOngoingCallPendingIntent(
        callId: StreamCallId,
        notificationId: Int,
        payload: Map<String, Any?>,
    ): PendingIntent?
}
