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

package io.getstream.video.android.app.ui.outgoing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.logging.StreamLog
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.events.CallAcceptedEvent
import io.getstream.video.android.events.CallRejectedEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.OutgoingCallData
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.model.toMetadata
import io.getstream.video.android.router.StreamRouter
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccessSuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutgoingCallViewModel(
    private val streamVideo: StreamVideo,
    private val streamRouter: StreamRouter,
    private val callData: OutgoingCallData
) : ViewModel(), SocketListener {

    private val logger by lazy { StreamLog.getLogger("OutgoingCallViewModel") }

    private var callRejectionCount = 0

    // TODO - we should use these to change UI and then build call settings when we join a call
    private val _isMicrophoneEnabled = MutableStateFlow(false)
    public val isMicrophoneEnabled: StateFlow<Boolean> = _isMicrophoneEnabled

    private val _isVideoEnabled = MutableStateFlow(false)
    public val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled

    init {
        streamVideo.addSocketListener(this)
        viewModelScope.launch {
            streamVideo.callState.collect { state ->
                when (state) {
                    is StreamCallState.Idle -> {
                        logger.i { "[observeState] state: Idle" }
                        streamVideo.clearCallState()
                        streamRouter.finish()
                    }
                    is StreamCallState.Active -> {}
                }
            }
        }
    }

    override fun onEvent(event: VideoEvent) {
        super.onEvent(event)
        when (event) {
            is CallAcceptedEvent -> joinCall()
            is CallRejectedEvent -> onCallRejected()
            else -> Unit
        }
    }

    private fun onCallRejected() {
        callRejectionCount += 1
        logger.d { "[onCallRejected] rejected call count $callRejectionCount" }
        if (callRejectionCount == (callData.users.count() - 1)) {
            logger.d { "[onCallRejected] Hanging up call" }
            hangUpCall()
        }
    }

    fun hangUpCall() {
        viewModelScope.launch {
            streamVideo.cancelCall(callData.callInfo.cid)
        }
    }

    private fun joinCall() {
        viewModelScope.launch {
            val joinResult = streamVideo.joinCall(
                callData.toMetadata()
            )

            joinResult.onSuccessSuspend { response ->
                streamRouter.navigateToCall(
                    callInput = CallInput(
                        response.call.cid,
                        response.call.type,
                        response.call.id,
                        response.callUrl,
                        response.userToken,
                        response.iceServers
                    ),
                    finishCurrent = true
                )
            }
            joinResult.onError {
                Log.d("Couldn't select server", it.message ?: "")
                streamRouter.onCallFailed(it.message)
            }
        }
    }

    fun getUserId(): String {
        return streamVideo.getUser().id
    }

    override fun onCleared() {
        streamVideo.removeSocketListener(this)
        super.onCleared()
    }

    fun onMicrophoneChanged(microphoneEnabled: Boolean) {
        this._isMicrophoneEnabled.value = microphoneEnabled
    }

    fun onVideoChanged(videoEnabled: Boolean) {
        this._isVideoEnabled.value = videoEnabled
    }
}
