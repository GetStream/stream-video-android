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

package io.getstream.video.android.core.notifications.internal.service

import android.app.Notification
import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.R
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.model.StreamCallId

internal class ServiceNotificationRetriever {
    private val logger by taggedLogger("ServiceNotificationRetriever")

    /**
     * Builds a notification and its corresponding notification ID for a given call trigger.
     *
     * This method is responsible for creating (or updating) the call-related notification
     * based on the provided trigger and current call context.
     *
     * @param context The Android [Context] used to build the notification.
     * @param trigger A string indicating the reason for the notification update
     *                eg. [CallService.TRIGGER_INCOMING_CALL], [CallService.TRIGGER_ONGOING_CALL], [CallService.TRIGGER_OUTGOING_CALL]
     * @param streamVideo The active [StreamVideoClient] instance used to access call and SDK state.
     * @param streamCallId The unique identifier of the call this notification belongs to.
     * @param intentCallDisplayName Optional display name for the call, typically
     *                              shown in the notification UI.
     *
     * @return A [Pair] where:
     * - **first**: The [Notification] to be displayed, or `null` if no notification
     *   should be shown for the given trigger.
     * - **second**: The notification ID used to post or update the notification.
     */
    fun getNotificationPair(
        context: Context,
        trigger: CallService.Companion.Trigger,
        streamVideo: StreamVideoClient,
        streamCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        logger.d {
            "[getNotificationPair] trigger: $trigger, callId: ${streamCallId.id}, callDisplayName: $intentCallDisplayName"
        }
        val call = streamVideo.call(streamCallId.type, streamCallId.id)
        val notificationData: Pair<Notification?, Int> = when (trigger) {
            CallService.Companion.Trigger.OnGoingCall -> {
                logger.d { "[getNotificationPair] Creating ongoing call notification" }
                val notificationId = call.state.notificationIdFlow.value
                    ?: streamCallId.getNotificationId(NotificationType.Ongoing)

                Pair(
                    first = streamVideo.getOngoingCallNotification(
                        callId = streamCallId,
                        callDisplayName = intentCallDisplayName,
                        payload = emptyMap(),
                    ),
                    second = notificationId,
                )
            }

            CallService.Companion.Trigger.IncomingCall -> {
                val shouldHaveContentIntent = streamVideo.state.activeCall.value == null
                logger.d { "[getNotificationPair] Creating incoming call notification" }
                val notificationId = call.state.notificationIdFlow.value
                    ?: streamCallId.getNotificationId(NotificationType.Incoming)

                Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Incoming(),
                        callId = streamCallId,
                        callDisplayName = intentCallDisplayName,
                        shouldHaveContentIntent = shouldHaveContentIntent,
                        payload = emptyMap(),
                    ),
                    second = notificationId,
                )
            }

            CallService.Companion.Trigger.OutgoingCall -> {
                logger.d { "[getNotificationPair] Creating outgoing call notification" }
                val notificationId = call.state.notificationIdFlow.value
                    ?: streamCallId.getNotificationId(NotificationType.Outgoing)

                Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Outgoing(),
                        callId = streamCallId,
                        callDisplayName = context.getString(
                            R.string.stream_video_outgoing_call_notification_title,
                        ),
                        payload = emptyMap(),
                    ),
                    second = notificationId,
                )
            }

            CallService.Companion.Trigger.RemoveIncomingCall -> {
                logger.d { "[getNotificationPair] Removing incoming call notification" }
                Pair(null, streamCallId.getNotificationId(NotificationType.Incoming))
            }

            else -> {
                logger.w { "[getNotificationPair] Unknown trigger: $trigger" }
                Pair(null, streamCallId.hashCode())
            }
        }
        logger.d {
            "[getNotificationPair] Generated notification: ${notificationData.first != null}, notificationId: ${notificationData.second}"
        }
        return notificationData
    }

    internal fun getNotificationId(
        trigger: CallService.Companion.Trigger,
        streamVideo: StreamVideo,
        streamCallId: StreamCallId,
    ): Int {
        val call = streamVideo.call(streamCallId.type, streamCallId.id)
        return when (trigger) {
            CallService.Companion.Trigger.OnGoingCall ->
                call.state.notificationIdFlow.value
                    ?: streamCallId.getNotificationId(NotificationType.Ongoing)

            CallService.Companion.Trigger.IncomingCall ->
                call.state.notificationIdFlow.value
                    ?: streamCallId.getNotificationId(NotificationType.Incoming)

            CallService.Companion.Trigger.OutgoingCall ->
                call.state.notificationIdFlow.value
                    ?: streamCallId.getNotificationId(NotificationType.Outgoing)

            CallService.Companion.Trigger.RemoveIncomingCall -> streamCallId.getNotificationId(
                NotificationType.Incoming,
            )
            else -> streamCallId.hashCode()
        }
    }

    fun notificationConfig(): NotificationConfig {
        val client = StreamVideo.instanceOrNull() as StreamVideoClient
        return client.streamNotificationManager.notificationConfig
    }
}
