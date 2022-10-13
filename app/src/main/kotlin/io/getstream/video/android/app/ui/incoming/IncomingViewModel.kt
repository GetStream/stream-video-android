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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.model.CallInfo
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.model.callId
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import stream.video.coordinator.client_v1_rpc.UserEventType
import io.getstream.video.android.model.state.StreamCallState as State

class IncomingViewModel(
    private val streamCalls: StreamCalls
) : ViewModel() {

    private val logger = StreamLog.getLogger("Call:Incoming-VM")

    private val _incomingData = MutableStateFlow<IncomingCallData?>(null)
    val incomingData: StateFlow<IncomingCallData?> = _incomingData

    private val _acceptedEvent = MutableSharedFlow<AcceptedIncomingCallData>()
    val acceptedEvent: SharedFlow<AcceptedIncomingCallData> = _acceptedEvent

    private val _dropEvent = MutableSharedFlow<Unit>()
    val dropEvent: SharedFlow<Unit> = _dropEvent

    private val _errorEvent = MutableSharedFlow<Unit>()
    val errorEvent: SharedFlow<Unit> = _errorEvent

    init {
        scheduleTimer()
        observeCallState()
    }

    private fun scheduleTimer() {
        viewModelScope.launch {
            delay(timeMillis = 10_000)
            _dropEvent.emit(Unit)
        }
    }

    private fun observeCallState() {
        viewModelScope.launch {
            streamCalls.callState.collect { state ->
                logger.i { "[observeCallState] state: $state" }
                when (state) {
                    is State.Incoming -> {
                        _incomingData.emit(
                            IncomingCallData(
                                callInfo = state.info,
                                callType = CallType.fromType(state.info.type),
                                participants = state.users.values.toList()
                                    .filter { it.id != streamCalls.getUser().id }
                            )
                        )
                    }
                    else -> {
                        _dropEvent.emit(Unit)
                    }
                }
            }
        }
    }

    fun hangUp() {
        val data = _incomingData.value ?: return
        viewModelScope.launch {
            val result = streamCalls.sendEvent(
                data.callInfo.callId,
                data.callInfo.type,
                UserEventType.USER_EVENT_TYPE_REJECTED_CALL
            )
            streamCalls.leaveCall()

            logger.d { "[hangUp] result: $result" }
            _dropEvent.emit(Unit)
        }
    }

    fun pickUp() {
        val data = _incomingData.value ?: return
        viewModelScope.launch {
            val callType = data.callInfo.type
            val callId = data.callInfo.callId
            logger.d { "[pickUp] callType: $callType, callId: $callId" }
            val acceptResult = streamCalls.acceptCall(callType, callId)
            logger.v { "[pickUp] result: $acceptResult" }
            when (acceptResult) {
                is Success -> {
                    val joinData = acceptResult.data
                    _acceptedEvent.emit(
                        AcceptedIncomingCallData(
                            callCid = data.callInfo.cid,
                            signalUrl = joinData.callUrl,
                            userToken = joinData.userToken,
                            iceServers = joinData.iceServers
                        )
                    )
                    _dropEvent.emit(Unit)
                }
                is Failure -> {
                    _errorEvent.emit(Unit)
                    hangUp()
                }
            }
        }
    }
}

class IncomingViewModelFactory(
    private val streamCalls: StreamCalls
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IncomingViewModel(
            streamCalls = streamCalls,
        ) as T
    }
}

data class IncomingCallData(
    val callType: CallType,
    val callInfo: CallInfo,
    val participants: List<CallUser>
)

data class AcceptedIncomingCallData(
    val callCid: String,
    val signalUrl: String,
    val userToken: String,
    val iceServers: List<IceServer>
)
