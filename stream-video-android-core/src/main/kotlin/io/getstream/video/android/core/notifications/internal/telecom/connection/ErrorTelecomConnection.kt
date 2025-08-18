package io.getstream.video.android.core.notifications.internal.telecom.connection

import android.content.Context
import android.telecom.Connection

class ErrorTelecomConnection(
    val context: Context,
) : Connection() {

    override fun onAnswer() {
        super.onAnswer()
    }

    override fun onReject() {
        super.onReject()
    }

    override fun onAnswer(videoState: Int) {
        super.onAnswer(videoState)
    }

    override fun onAbort() {
        super.onAbort()
    }

    override fun onDisconnect() {
        super.onDisconnect()
    }
}

