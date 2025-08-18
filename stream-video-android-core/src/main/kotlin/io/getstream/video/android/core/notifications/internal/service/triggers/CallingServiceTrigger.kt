package io.getstream.video.android.core.notifications.internal.service.triggers

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.notifications.internal.service.TelecomVoipService
import io.getstream.video.android.model.StreamCallId

interface CallingServiceTrigger {

    fun showIncomingCall(
        context: Context,
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
        isVideo:Boolean,
        payload:Map<String, Any?>,
        notification: Notification?,
    )

    fun getServiceClass(): Class<*> {
        val canUseTelecom = true //TODO Rahul hardcoded check
        return if (canUseTelecom) {
            TelecomVoipService::class.java
        } else
            CallService::class.java
    }
}