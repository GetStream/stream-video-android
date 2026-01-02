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
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
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
        logInput(callId, callDisplayName, notification)

        val startParams = StartServiceParam(
            callId = callId,
            trigger = TRIGGER_INCOMING_CALL,
            callDisplayName = callDisplayName,
            callServiceConfiguration = callServiceConfiguration,
        )

        var result = ShowIncomingCallResult.ERROR
        safeCallWithResult {
            if (hasNoActiveCall()) {
                startForegroundService(context, startParams)
                result = ShowIncomingCallResult.FG_SERVICE
            } else {
                result = handleWhileActiveCall(context, startParams, notification)
            }
        }.onError { error ->
            logger.d { "[showIncomingCall] onError" }
            result = showNotification(context, notification, callId, error)
        }
        return result
    }

    // ----------------------------------
    // Decision branches
    // ----------------------------------

    private fun handleWhileActiveCall(
        context: Context,
        startParams: StartServiceParam,
        notification: Notification?,
    ): ShowIncomingCallResult {
        val serviceClass = startParams.callServiceConfiguration.serviceClass

        return if (serviceIntentBuilder.isServiceRunning(context, serviceClass)) {
            showNotification(context, notification, startParams.callId, null)
        } else {
            logger.d { "[showIncomingCall] Starting regular service" }
            context.startService(
                serviceIntentBuilder.buildStartIntent(context, startParams),
            )
            ShowIncomingCallResult.SERVICE
        }
    }

    // ----------------------------------
    // Side effects
    // ----------------------------------

    private fun startForegroundService(
        context: Context,
        params: StartServiceParam,
    ) {
        logger.d { "[showIncomingCall] Starting foreground service" }
        ContextCompat.startForegroundService(
            context,
            serviceIntentBuilder.buildStartIntent(context, params),
        )
    }

    private fun showNotification(
        context: Context,
        notification: Notification?,
        callId: StreamCallId,
        error: Any?,
    ): ShowIncomingCallResult {
        if (!hasNotificationPermission(context) || notification == null) {
            logger.w {
                "[showIncomingCall] Cannot show notification - " +
                    "permission=${hasNotificationPermission(context)}, " +
                    "notification=${notification != null}"
            }
            return ShowIncomingCallResult.ERROR
        }

        StreamVideo.instanceOrNull()
            ?.getStreamNotificationDispatcher()
            ?.notify(
                callId,
                callId.getNotificationId(NotificationType.Incoming),
                notification,
            )

        return ShowIncomingCallResult.ONLY_NOTIFICATION
    }

    // ----------------------------------
    // State / helpers
    // ----------------------------------

    private fun hasNoActiveCall(): Boolean {
        val hasActiveCall =
            StreamVideo.instanceOrNull()?.state?.activeCall?.value != null
        logger.d { "[showIncomingCall] hasActiveCall: $hasActiveCall" }
        return !hasActiveCall
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun logInput(
        callId: StreamCallId,
        callDisplayName: String?,
        notification: Notification?,
    ) {
        logger.d {
            "[showIncomingCall] callId=${callId.id}, " +
                "callDisplayName=$callDisplayName, " +
                "notification=${notification != null}"
        }
    }
}

internal enum class ShowIncomingCallResult {
    FG_SERVICE, SERVICE, ONLY_NOTIFICATION, ERROR
}
