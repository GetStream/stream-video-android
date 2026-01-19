/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal.service.managers

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CallServiceLifecycleManager {
    private val logger by taggedLogger("CallServiceLifecycleManager")
    fun initializeCallAndSocket(
        scope: CoroutineScope,
        streamVideo: StreamVideo,
        callId: StreamCallId,
        onError: () -> Unit,
    ) {
        scope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            val update = call.get()

            if (update.isFailure) {
                onError()
                return@launch
            }
        }

        scope.launch {
            streamVideo.connectIfNotAlreadyConnected()
        }
    }

    fun updateRingingCall(
        scope: CoroutineScope,
        streamVideo: StreamVideo,
        callId: StreamCallId,
        ringingState: RingingState,
    ) {
        scope.launch {
            val call = streamVideo.call(callId.type, callId.id)
            streamVideo.state.addRingingCall(call, ringingState)
        }
    }

    fun endCall(scope: CoroutineScope, callId: StreamCallId?) {
        callId?.let { id ->
            StreamVideo.Companion.instanceOrNull()?.let { streamVideo ->
                val call = streamVideo.call(id.type, id.id)
                val ringingState = call.state.ringingState.value

                when (ringingState) {
                    is RingingState.Outgoing -> {
                        scope.launch {
                            call.reject(
                                "CallService.EndCall",
                                RejectReason.Custom("Android Service Task Removed"),
                            )
                            logger.i { "[onTaskRemoved] Ended outgoing call for all users" }
                        }
                    }

                    is RingingState.Incoming -> {
                        handleIncomingCallTaskRemoved(scope, call)
                    }

                    else -> {
                        call.leave("call-service-end-call-unknown")
                        logger.i { "[onTaskRemoved] Ended ongoing call for me" }
                    }
                }
            }
        }
    }

    private fun handleIncomingCallTaskRemoved(scope: CoroutineScope, call: Call) {
        val memberCount = call.state.members.value.size
        logger.i { "[handleIncomingCallTaskRemoved] Total members: $memberCount" }

        if (memberCount == 2) {
            scope.launch {
                call.reject(source = "memberCount == 2")
                logger.i { "[handleIncomingCallTaskRemoved] Ended incoming call for both users" }
            }
        } else {
            call.leave("call-service-end-call-incoming")
            logger.i { "[handleIncomingCallTaskRemoved] Ended incoming call for me" }
        }
    }
}
