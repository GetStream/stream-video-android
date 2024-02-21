package io.getstream.video.android.core.notifications.internal.service

import androidx.annotation.StringDef
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_INCOMING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_ONGOING_CALL
import io.getstream.video.android.core.notifications.internal.service.CallTriggers.TRIGGER_OUTGOING_CALL

@Retention(AnnotationRetention.SOURCE)
@StringDef(
        TRIGGER_INCOMING_CALL,
        TRIGGER_OUTGOING_CALL,
        TRIGGER_ONGOING_CALL
)
internal annotation class CallTrigger()

internal object CallTriggers {
    const val TRIGGER_INCOMING_CALL = "incoming_call"
    const val TRIGGER_OUTGOING_CALL = "outgoing_call"
    const val TRIGGER_ONGOING_CALL = "ongoing_call"
}
