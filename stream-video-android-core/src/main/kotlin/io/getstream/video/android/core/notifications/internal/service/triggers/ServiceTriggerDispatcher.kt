package io.getstream.video.android.core.notifications.internal.service.triggers

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.telecom.CallsManager
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INCOMING_CALL_NOTIFICATION_ID
import io.getstream.video.android.core.notifications.internal.service.CallService.Companion.TRIGGER_REMOVE_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.StartServiceParam
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.StreamCallId

/**
 * TODO Rahul change the name of the claass its a decision maker class which will decide which
 * which service to pick
 */
class ServiceTriggerDispatcher(val context: Context) {

    private val logger by taggedLogger("ServiceTriggers")
    private val serviceIntentBuilder = ServiceIntentBuilder()
    private val legacyServiceTrigger = LegacyServiceTrigger(serviceIntentBuilder)

    @SuppressLint("NewApi")
    private val callsManager =
        CallsManager(context) //TODO Rahul risk - remove @SuppressLint("NewApi")
    private val rawTelecomServiceTrigger = RawTelecomServiceTrigger(context, serviceIntentBuilder)

    fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig,
        isVideo: Boolean,
        payload: Map<String, Any?>,
        streamVideo: StreamVideo,
        notification: Notification?,
    ) {

        if (isTelecomSupported()) {

            rawTelecomServiceTrigger.addIncomingCallToTelecom(
                context,
                callId,
                callDisplayName,
                callServiceConfiguration,
                isVideo,
                payload,
                streamVideo,
                notification
            )
        }

        legacyServiceTrigger.showIncomingCall(
            context,
            callId,
            callDisplayName,
            callServiceConfiguration,
            isVideo,
            payload,
            notification
        )

    }

    fun removeIncomingCall(
        context: Context,
        callId: StreamCallId,
        config: CallServiceConfig = DefaultCallConfigurations.default,
    ) {
        safeCallWithResult {
            context.startService(
                serviceIntentBuilder.buildStartIntent(
                    context,
                    StartServiceParam(
                        callId,
                        TRIGGER_REMOVE_INCOMING_CALL,
                        callServiceConfiguration = config,
                    )
                ),
            )!!
        }.onError {
            NotificationManagerCompat.from(context).cancel(INCOMING_CALL_NOTIFICATION_ID)
        }
    }

    fun isTelecomSupported(): Boolean = true //TODO Rahul hardcoded remove later

}


