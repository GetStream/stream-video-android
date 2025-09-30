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
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.IncomingNotificationAction
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.triggers.IncomingCallPresenter
import io.getstream.video.android.model.StreamCallId

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
        streamVideo.call(callId.type, callId.id).leave()
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
