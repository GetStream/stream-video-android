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

package io.getstream.video.android.core.notifications.internal.receivers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.ACTION_REJECT_CALL
import io.getstream.video.android.core.notifications.internal.service.triggers.CallServiceLauncherImpl
import io.getstream.video.android.model.StreamCallId

/**
 * Used to process any pending intents that feature the [ACTION_REJECT_CALL] action. By consuming this
 * event, it rejects a call without starting the application UI, notifying other participants that
 * this user won't join the call. After which it dismisses the originating notification.
 */
internal class RejectCallBroadcastReceiver : GenericCallActionBroadcastReceiver() {

    val logger by taggedLogger("Call:RejectReceiver")
    override val action = ACTION_REJECT_CALL

    override suspend fun onReceive(call: Call, context: Context, intent: Intent) {
        when (val rejectResult = call.reject(RejectReason.Decline)) {
            is Result.Success -> {
                val userId = StreamVideo.instanceOrNull()?.userId
                userId?.let {
                    val set = mutableSetOf(it)
                    call.state.updateRejectedBy(set)
                    call.state.updateRejectActionBundle(intent.extras ?: Bundle())
                }
                logger.d { "[onReceive] rejectCall, Success: $rejectResult" }
            }
            is Result.Failure -> {
                logger.d { "[onReceive] rejectCall, Failure: $rejectResult" }
            }
        }
        logger.d { "[onReceive] #ringing; callId: ${call.id}, action: ${intent.action}" }

        CallServiceLauncherImpl()
            .removeIncomingCall(
                context,
                StreamCallId.fromCallCid(call.cid),
                StreamVideo.instance().state.callConfigRegistry.get(call.type),
            )
    }
}
