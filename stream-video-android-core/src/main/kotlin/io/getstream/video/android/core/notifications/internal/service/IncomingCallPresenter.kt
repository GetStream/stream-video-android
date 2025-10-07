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

import android.Manifest
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.telecom.StartServiceParam
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId

internal class IncomingCallPresenter(private val serviceIntentBuilder: ServiceIntentBuilder) {
    private val logger by taggedLogger("IncomingCallPresenter")

    fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig,
        notification: Notification?,
    ): ShowIncomingCallResult {
        logger.d {
            "[showIncomingCall] callId: ${callId.id}, callDisplayName: $callDisplayName, notification: ${notification != null}"
        }
        val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null
        logger.d { "[showIncomingCall] hasActiveCall: $hasActiveCall" }
        var showIncomingCallResult = ShowIncomingCallResult.ERROR
        safeCallWithResult {
            if (!hasActiveCall) {
                logger.d { "[showIncomingCall] Starting foreground service" }
                ContextCompat.startForegroundService(
                    context,
                    serviceIntentBuilder.buildStartIntent(
                        context,
                        StartServiceParam(
                            callId,
                            TRIGGER_INCOMING_CALL,
                            callDisplayName,
                            callServiceConfiguration,
                        ),
                    ),
                )
                ComponentName(context, CallService::class.java)
                showIncomingCallResult = ShowIncomingCallResult.FG_SERVICE
            } else {
                logger.d { "[showIncomingCall] Starting regular service" }
                context.startService(
                    serviceIntentBuilder.buildStartIntent(
                        context,
                        StartServiceParam(
                            callId,
                            TRIGGER_INCOMING_CALL,
                            callDisplayName,
                            callServiceConfiguration,
                        ),
                    ),
                )
                showIncomingCallResult = ShowIncomingCallResult.SERVICE
            }
        }.onError {
            // Show notification
            logger.e { "Could not start service, showing notification only: $it" }
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            logger.i { "Has permission: $hasPermission" }
            logger.i { "Notification: $notification" }
            if (hasPermission && notification != null) {
                logger.d {
                    "[showIncomingCall] Showing notification fallback with ID: ${callId.getNotificationId(
                        NotificationType.Incoming,
                    )}"
                }
                StreamVideo.instanceOrNull()?.getStreamNotificationDispatcher()?.notify(
                    callId,
                    callId.getNotificationId(NotificationType.Incoming),
                    notification,
                )
                showIncomingCallResult = ShowIncomingCallResult.ONLY_NOTIFICATION
            } else {
                logger.w {
                    "[showIncomingCall] Cannot show notification - hasPermission: $hasPermission, notification: ${notification != null}"
                }
            }
        }
        return showIncomingCallResult
    }
}

internal enum class ShowIncomingCallResult {
    FG_SERVICE, SERVICE, ONLY_NOTIFICATION, ERROR
}
