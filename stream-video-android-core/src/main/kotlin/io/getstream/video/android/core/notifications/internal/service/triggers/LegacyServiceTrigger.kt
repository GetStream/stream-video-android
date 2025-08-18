package io.getstream.video.android.core.notifications.internal.service.triggers

import android.Manifest
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.StartServiceParam
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId

internal class LegacyServiceTrigger(val serviceIntentBuilder: ServiceIntentBuilder) :
    CallingServiceTrigger {
    private val logger by taggedLogger("ServiceTriggers")
    override fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig,
        isVideo:Boolean,
        payload:Map<String, Any?>,
        notification: Notification?,
    ) {
        logger.d {
            "[showIncomingCall] callId: ${callId.id}, callDisplayName: $callDisplayName, notification: ${notification != null}"
        }
        val hasActiveCall = StreamVideo.instanceOrNull()?.state?.activeCall?.value != null
        logger.d { "[showIncomingCall] hasActiveCall: $hasActiveCall" }
        safeCallWithResult {
            val result = if (!hasActiveCall) {
                logger.d { "[showIncomingCall] Starting foreground service" }
                ContextCompat.startForegroundService(
                    context,
                    serviceIntentBuilder.buildStartIntent(
                        context,
                        StartServiceParam(callId,
                            TRIGGER_INCOMING_CALL,
                            callDisplayName,
                            callServiceConfiguration,
                        )
                    ),
                )
                ComponentName(context, getServiceClass())
            } else {
                logger.d { "[showIncomingCall] Starting regular service" }
                context.startService(
                    serviceIntentBuilder.buildStartIntent(
                        context,
                        StartServiceParam(callId,
                            TRIGGER_INCOMING_CALL,
                            callDisplayName,
                            callServiceConfiguration,
                        )
                    ),
                )
            }
            result!!
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
                    "[showIncomingCall] Showing notification fallback with ID: $INCOMING_CALL_NOTIFICATION_ID"
                }
                StreamVideo.instanceOrNull()?.getStreamNotificationDispatcher()?.notify(
                    callId,
                    INCOMING_CALL_NOTIFICATION_ID,
                    notification,
                )
            } else {
                logger.w {
                    "[showIncomingCall] Cannot show notification - hasPermission: $hasPermission, notification: ${notification != null}"
                }
            }
        }
    }

}