package io.getstream.video.android.core.notifications.internal.telecom.notificationtrigger

import android.app.Notification
import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.model.StreamCallId

class TelecomSystemOwnedNotificationTrigger(val serviceIntentBuilder: ServiceIntentBuilder = ServiceIntentBuilder()) {

    private val logger by taggedLogger("TelecomSystemOwnedNotificationTrigger")

    fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
        notification: Notification?,
    ) {
    }

}