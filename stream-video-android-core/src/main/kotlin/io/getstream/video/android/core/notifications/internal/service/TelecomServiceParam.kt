package io.getstream.video.android.core.notifications.internal.service

import io.getstream.video.android.model.StreamCallId

data class StartServiceParam (val callId: StreamCallId,
                              val trigger: String,
                              val callDisplayName: String? = null,
                              val callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default)

data class StopServiceParam (val callServiceConfiguration: CallServiceConfig = DefaultCallConfigurations.default)