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
import io.getstream.video.android.core.notifications.handlers.StreamDefaultNotificationHandler
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_OUTGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.model.StreamCallId

internal class ServiceNotificationRetriever {
    private val logger by taggedLogger("ServiceNotificationRetriever")

    open fun getNotificationPair(
        context: Context,
        trigger: String,
        streamVideo: StreamVideoClient,
        streamCallId: StreamCallId,
        intentCallDisplayName: String?,
    ): Pair<Notification?, Int> {
        logger.d {
            "[getNotificationPair] trigger: $trigger, callId: ${streamCallId.id}, callDisplayName: $intentCallDisplayName"
        }
        val notificationData: Pair<Notification?, Int> = when (trigger) {
            TRIGGER_ONGOING_CALL -> {
                logger.d { "[getNotificationPair] Creating ongoing call notification" }
                Pair(
                    first = streamVideo.getOngoingCallNotification(
                        callId = streamCallId,
                        callDisplayName = intentCallDisplayName,
                        payload = emptyMap(),
                    ),
                    second = streamCallId.hashCode(),
                )
            }

            TRIGGER_INCOMING_CALL -> {
                logger.d { "[getNotificationPair] Creating incoming call notification" }
                val shouldHaveContentIntent = streamVideo.state.activeCall.value == null
                logger.d { "[getNotificationPair] shouldHaveContentIntent: $shouldHaveContentIntent" }
                Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Incoming(),
                        callId = streamCallId,
                        callDisplayName = intentCallDisplayName,
                        shouldHaveContentIntent = shouldHaveContentIntent,
                        payload = emptyMap(),
                    ),
                    second = streamCallId.getNotificationId(NotificationType.Incoming),
                )
            }

            TRIGGER_OUTGOING_CALL -> {
                logger.d { "[getNotificationPair] Creating outgoing call notification" }
                Pair(
                    first = streamVideo.getRingingCallNotification(
                        ringingState = RingingState.Outgoing(),
                        callId = streamCallId,
                        callDisplayName = context.getString(
                            R.string.stream_video_outgoing_call_notification_title,
                        ),
                        payload = emptyMap(),
                    ),
                    second = streamCallId.getNotificationId(
                        NotificationType.Incoming, // TODO Rahul, should we change it to outgoing?
                    ), // Same for incoming and outgoing
                )
            }

            TRIGGER_REMOVE_INCOMING_CALL -> {
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

    fun notificationConfig(): NotificationConfig {
        val client = StreamVideo.instanceOrNull() as StreamVideoClient
        return client.streamNotificationManager.notificationConfig
    }
}
