package io.getstream.video.android.core.notifications.internal.service

import io.getstream.video.android.core.Call
import io.getstream.video.android.model.StreamCallId

data class StartServiceParam(
    val callId: StreamCallId,
    val trigger: String,
    val callDisplayName: String? = null,
    val callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
)

data class StopServiceParam(
    val call: Call? = null,
    val callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default,
)

sealed class StopForegroundServiceSource(val source: String) {
    data object CallAccept : StopForegroundServiceSource("accept the call")
    data object SetActiveCall : StopForegroundServiceSource("set active call")
    data object RemoveActiveCall : StopForegroundServiceSource("remove active call")
    data object RemoveRingingCall : StopForegroundServiceSource("remove ringing call")
}
