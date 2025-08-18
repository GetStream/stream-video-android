package io.getstream.video.android.core.notifications.internal.service

import android.content.Intent
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.model.StreamCallId

internal interface CallingServiceContract {
    fun maybeHandleMediaIntent(intent: Intent?, callId: StreamCallId?)
    fun maybePromoteToForegroundService(
        videoClient: StreamVideoClient,
        notificationId: Int,
        trigger: String,
    )

    fun stopService()
    fun initializeCallAndSocket(
        streamVideo: StreamVideo,
        callId: StreamCallId,
    )
}