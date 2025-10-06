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

package io.getstream.video.android.core.notifications.internal.telecom

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.ExternalCallRejectionHandler
import io.getstream.video.android.core.ExternalCallRejectionSource
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.IncomingNotificationAction
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.IncomingCallPresenter
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

class IncomingCallTelecomAction(
    private val context: Context,
    private val streamVideo: StreamVideo,
    private val incomingCallPresenter: IncomingCallPresenter,
) {

    fun onAnswer(callId: StreamCallId) {
        val pendingIntentMap = streamVideo.call(callId.type, callId.id)
            .state.incomingNotificationData.pendingIntentMap

        pendingIntentMap[IncomingNotificationAction.Accept]?.send()
    }

    fun onDisconnect(callId: StreamCallId) {
        val call = streamVideo.call(callId.type, callId.id)
        when (call.state.ringingState.value) {
            is RingingState.Outgoing -> {
                call.scope.launch {
                    val externalCallRejectionHandler = ExternalCallRejectionHandler()
                    externalCallRejectionHandler.onRejectCall(
                        ExternalCallRejectionSource.WEARABLE,
                        call,
                        streamVideo.context,
                    )
                }
            }

            is RingingState.Active -> {
                streamVideo.call(callId.type, callId.id).leave()
            }
            is RingingState.Incoming -> {
                val pendingIntentMap = streamVideo.call(callId.type, callId.id)
                    .state.incomingNotificationData.pendingIntentMap

                pendingIntentMap[IncomingNotificationAction.Reject]?.send()
            }
            else -> {}
        }
    }

    fun onShowIncomingCallUi(
        callId: StreamCallId,
        callDisplayName: String?,
        callServiceConfiguration: CallServiceConfig,
        notification: Notification?,
    ) {
        incomingCallPresenter.showIncomingCall(
            context,
            callId,
            callDisplayName,
            callServiceConfiguration,
            notification,
        )
    }
}
