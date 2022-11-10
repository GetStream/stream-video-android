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

package io.getstream.video.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.model.CallEventType
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.router.StreamRouter
import io.getstream.video.android.utils.flatMap
import io.getstream.video.android.utils.map
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import io.getstream.video.android.model.state.StreamCallState as State

public class IncomingCallViewModel(
    private val streamVideo: StreamVideo,
    private val streamRouter: StreamRouter,
) : ViewModel() {

    private val logger = StreamLog.getLogger("Call:Incoming-VM")

    private val _callType: MutableStateFlow<CallType> = MutableStateFlow(CallType.VIDEO)
    private val _participants: MutableStateFlow<List<CallUser>> = MutableStateFlow(emptyList())

    public val callType: StateFlow<CallType> = _callType
    public val participants: StateFlow<List<CallUser>> = _participants

    init {
        viewModelScope.launch {
            streamVideo.callState.collect { state ->
                when (state) {
                    is State.Idle -> {
                        logger.i { "[observeState] state: Idle" }
                        streamVideo.clearCallState()
                        streamRouter.finish()
                    }
                    is State.Incoming -> {
                        _callType.value = CallType.fromType(state.callGuid.type)
                        _participants.value = state.users.values.toList()
                    }
                    is State.Active -> {}
                }
            }
        }
    }

    public fun acceptCall() {
        val state = streamVideo.callState.value
        if (state !is State.Incoming || state.acceptedByMe) {
            logger.w { "[acceptCall] rejected (state is not unaccepted Incoming): $state" }
            return
        }
        logger.d { "[acceptCall] state: $state" }
        viewModelScope.launch {
            streamVideo.joinCall(state.callGuid.type, state.callGuid.id)
                .flatMap { joined ->
                    logger.v { "[acceptCall] joined: $joined" }
                    streamVideo.sendEvent(
                        callCid = joined.call.cid,
                        eventType = CallEventType.ACCEPTED
                    ).map { joined }
                }
                .onSuccess {
                    logger.v { "[acceptCall] completed: $it" }
                    streamRouter.navigateToCall(finishCurrent = true)
                }
                .onError {
                    logger.e { "[acceptCall] failed: $it" }
                    declineCall()
                }
        }
    }

    public fun declineCall() {
        val state = streamVideo.callState.value
        if (state !is State.Incoming || state.acceptedByMe) {
            logger.w { "[declineCall] rejected (state is not unaccepted Incoming): $state" }
            return
        }
        logger.d { "[declineCall] state: $state" }
        viewModelScope.launch {
            val result = streamVideo.rejectCall(state.callGuid.cid)
            logger.d { "[declineCall] result: $result" }
        }
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
