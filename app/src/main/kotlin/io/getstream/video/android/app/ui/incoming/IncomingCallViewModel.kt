/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.app.ui.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.events.CallCanceledEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.model.CallEventType
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.IncomingCallData
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.router.StreamRouter
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.utils.flatMap
import io.getstream.video.android.utils.map
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class IncomingCallViewModel(
    private val streamCalls: StreamCalls,
    private val streamRouter: StreamRouter,
    public val callData: IncomingCallData
) : ViewModel(), SocketListener {

    private val logger = StreamLog.getLogger("Call:Incoming-VM")

    init {
        streamCalls.addSocketListener(this)
        viewModelScope.launch {
            streamCalls.callState.collect { state ->
                when (state) {
                    is StreamCallState.Idle -> {
                        logger.i { "[observeState] state: Idle" }
                        streamCalls.clearCallState()
                        streamRouter.finish()
                    }
                    is StreamCallState.Active -> {}
                }
            }
        }
    }

    fun acceptCall() {
        val data = callData
        val callType = data.callInfo.type
        val callId = data.callInfo.id
        logger.d { "[acceptCall] callType: $callType, callId: $callId" }

        viewModelScope.launch {
            streamCalls.joinCall(callType, callId)
                .flatMap { joined ->
                    logger.v { "[acceptCall] joined: $joined" }
                    streamCalls.sendEvent(
                        callCid = joined.call.cid,
                        eventType = CallEventType.ACCEPTED
                    ).map { joined }
                }
                .onSuccess {
                    logger.v { "[acceptCall] completed: $it" }
                    streamRouter.navigateToCall(callInput = it.toCallInput(), finishCurrent = true)
                }
                .onError {
                    logger.e { "[acceptCall] failed: $it" }
                    declineCall()
                }
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            val result = streamCalls.rejectCall(callData.callInfo.cid)
            logger.d { "[declineCall] result: $result" }
        }
    }

    override fun onEvent(event: VideoEvent) {
        super.onEvent(event)
        logger.d { "[onEvent] $event" }
    }

    override fun onCleared() {
        streamCalls.removeSocketListener(this)
        super.onCleared()
    }

    private fun JoinedCall.toCallInput() = CallInput(
        callCid = call.cid,
        callType = call.type,
        callId = call.id,
        callUrl = callUrl,
        userToken = userToken,
        iceServers = iceServers
    )
}
