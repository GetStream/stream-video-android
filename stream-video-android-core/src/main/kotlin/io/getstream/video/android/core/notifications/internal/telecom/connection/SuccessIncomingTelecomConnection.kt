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

package io.getstream.video.android.core.notifications.internal.telecom.connection

import android.content.Context
import android.telecom.CallEndpoint
import android.telecom.Connection
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.telecom.IncomingCallTelecomAction
import io.getstream.video.android.core.notifications.internal.telecom.TelecomConnectionIncomingCallData

/**
 * Important apis to invoke
 * Connection.setRinging()
 * Connection.setActive() ~ for ongoing call
 * Connection.reject/decline
 */
class SuccessIncomingTelecomConnection(
    val context: Context,
    val streamVideo: StreamVideo,
    val telecomConnectionIncomingCallData: TelecomConnectionIncomingCallData,
    val incomingCallTelecomAction: IncomingCallTelecomAction,

) : Connection() {
    val logger by taggedLogger("SuccessIncomingTelecomConnection")

    /**
     * Accept from wearable
     */
    override fun onAnswer() {
        super.onAnswer()
        logger.d { "[onAnswer]" }

        with(telecomConnectionIncomingCallData) {
            incomingCallTelecomAction.onAnswer(callId)
        }
    }

    /**
     * Ongoing call is cancelled from wearable
     */
    override fun onDisconnect() {
        super.onDisconnect()
        logger.d { "[onDisconnect]" }
        with(telecomConnectionIncomingCallData) {
            incomingCallTelecomAction.onDisconnect(callId)
        }
    }

    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
        logger.d { "onShowIncomingCallUi" }
        telecomConnectionIncomingCallData.let {
            incomingCallTelecomAction.onShowIncomingCallUi(
                it.callId,
                it.callDisplayName,
                it.callServiceConfiguration,
                it.notification,
            )
        }
    }

    /**
     * Incoming call is rejected
     */
    override fun onReject() {
        super.onReject()
        logger.d { "[onReject]" }

//        with(telecomConnectionIncomingCallData) {
//            val pendingIntentMap = streamVideo.call(callId.type, callId.id)
//                .state.incomingNotificationData.pendingIntentMap
//
//            pendingIntentMap[IncomingNotificationAction.Reject]?.send()
//        }
    }

    /**
     * Incoming call is rejected
     */
    override fun onReject(rejectReason: Int) {
        super.onReject(rejectReason)
        logger.d { "onReject($rejectReason)" }
    }

    override fun onReject(replyMessage: String?) {
        super.onReject(replyMessage)
        logger.d { "onReject($replyMessage)" }
    }

    override fun onAnswer(videoState: Int) {
        super.onAnswer(videoState)
        logger.d { "[onAnswer($videoState)]" }
        // Start media track
    }

    override fun onAbort() {
        super.onAbort()
        logger.d { "[onAbort]" }
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        logger.d { "[onMuteStateChanged]" }
    }

    /**
     * Triggered when wearable will take ownership or leave ownership of voice
     */
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
    }

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
    }
}
