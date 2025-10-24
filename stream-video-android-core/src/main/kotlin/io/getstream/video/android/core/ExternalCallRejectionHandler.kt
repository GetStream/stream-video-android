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

package io.getstream.video.android.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.notifications.internal.service.ServiceLauncher
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.model.StreamCallId

internal class ExternalCallRejectionHandler() {
    private val logger by taggedLogger("CallRejectionHandler")

    suspend fun onRejectCall(source: ExternalCallRejectionSource, call: Call, context: Context, intent: Intent = Intent()) {
        when (
            val rejectResult = call.reject(
                source = "ExternalCallRejectionHandler.$source",
                RejectReason.Decline,
            )
        ) {
            is Result.Success -> {
                val userId = StreamVideo.instanceOrNull()?.userId
                userId?.let {
                    val set = mutableSetOf(it)
                    call.state.updateRejectedBy(set)
                    call.state.updateRejectActionBundle(intent.extras ?: Bundle())
                }
                logger.d { "[onRejectCall] source:$source rejectCall, Success: $rejectResult" }
            }
            is Result.Failure -> {
                logger.d { "[onRejectCall] source:$source, rejectCall, Failure: $rejectResult" }
            }
        }
        logger.d { "[onRejectCall] source:$source, #ringing; callId: ${call.id}, action: ${intent.action}" }

        val serviceLauncher = ServiceLauncher(context)
        serviceLauncher.removeIncomingCall(
            context,
            StreamCallId.fromCallCid(call.cid),
            StreamVideo.instance().state.callConfigRegistry.get(call.type),
        )
        when (source) {
            ExternalCallRejectionSource.NOTIFICATION -> {
                TelecomCallController(context)
                    .leaveCall(call)
            }
            ExternalCallRejectionSource.WEARABLE -> {
                /**
                 * Following the same logic from StreamCallActivity.reject(Call, RejectReason?,
                 *         onSuccess: (suspend (Call) -> Unit)?,
                 *         onError: (suspend (Exception) -> Unit)?,)
                 */
                call.leave()
            }
        }
    }
}

internal enum class ExternalCallRejectionSource {
    NOTIFICATION, WEARABLE
}
