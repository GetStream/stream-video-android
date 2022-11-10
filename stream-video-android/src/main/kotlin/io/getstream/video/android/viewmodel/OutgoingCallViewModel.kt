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
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.model.state.StreamDate
import io.getstream.video.android.router.StreamRouter
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import io.getstream.video.android.utils.onSuccessSuspend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

public class OutgoingCallViewModel(
    private val streamVideo: StreamVideo,
    private val streamRouter: StreamRouter,
) : ViewModel() {

    private val logger by lazy { StreamLog.getLogger("OutgoingCallViewModel") }

    // TODO - we should use these to change UI and then build call settings when we join a call
    private val _isMicrophoneEnabled = MutableStateFlow(false)
    public val isMicrophoneEnabled: StateFlow<Boolean> = _isMicrophoneEnabled

    private val _isVideoEnabled = MutableStateFlow(false)
    public val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled

    private val _callType: MutableStateFlow<CallType> = MutableStateFlow(CallType.VIDEO)
    private val _callId: MutableStateFlow<String> = MutableStateFlow(value = "")
    private val _participants: MutableStateFlow<List<CallUser>> = MutableStateFlow(emptyList())

    public val callType: StateFlow<CallType> = _callType
    public val callId: StateFlow<String> = _callId
    public val participants: StateFlow<List<CallUser>> = _participants

    init {
        viewModelScope.launch {
            streamVideo.callState.collect { state ->
                when (state) {
                    is StreamCallState.Idle -> {
                        logger.i { "[observeState] state: Idle" }
                        streamVideo.clearCallState()
                        streamRouter.finish()
                    }
                    is StreamCallState.Outgoing -> {
                        _callType.value = CallType.fromType(state.callGuid.type)
                        _callId.value = state.callGuid.id
                        _participants.value = state.users.values
                            .filter { it.id != streamVideo.getUser().id }
                            .toList()
                        joinCall()
                    }
                    is StreamCallState.Drop -> {
                        hangUpCall()
                    }
                    is StreamCallState.Active -> {}
                }
            }
        }
    }

    public fun hangUpCall() {
        val state = streamVideo.callState.value
        if (state !is StreamCallState.Outgoing) {
            logger.w { "[hangUpCall] rejected (state is not Outgoing): $state" }
            return
        }
        logger.d { "[hangUpCall] state: $state" }
        viewModelScope.launch {
            streamVideo.cancelCall(state.callGuid.cid)
        }
    }

    private fun joinCall() {
        val state = streamVideo.callState.value
        if (state !is StreamCallState.Outgoing || !state.acceptedByCallee) {
            logger.w { "[joinCall] rejected (state is not accepted Outgoing): $state" }
            return
        }
        logger.d { "[joinCall] state: $state" }
        viewModelScope.launch {
            val joinResult = streamVideo.joinCall(
                state.toMetadata()
            )

            joinResult.onSuccess {
                streamRouter.navigateToCall(
                    finishCurrent = true
                )
            }
            joinResult.onError {
                logger.e { "[joinCall] failed: $it" }
                streamRouter.onCallFailed(it.message)
            }
        }
    }

    public fun onMicrophoneChanged(microphoneEnabled: Boolean) {
        logger.d { "[onMicrophoneChanged] microphoneEnabled: $microphoneEnabled" }
        this._isMicrophoneEnabled.value = microphoneEnabled
    }

    public fun onVideoChanged(videoEnabled: Boolean) {
        logger.d { "[onVideoChanged] videoEnabled: $videoEnabled" }
        this._isVideoEnabled.value = videoEnabled
    }

    private fun StreamCallState.Outgoing.toMetadata(): CallMetadata =
        CallMetadata(
            cid = callGuid.cid,
            type = callGuid.type,
            id = callGuid.id,
            users = users,
            members = members,
            createdAt = (createdAt as? StreamDate.Specified)?.date?.time ?: 0,
            updatedAt = (updatedAt as? StreamDate.Specified)?.date?.time ?: 0,
            createdByUserId = createdByUserId,
            broadcastingEnabled = broadcastingEnabled,
            recordingEnabled = recordingEnabled,
            extraData = emptyMap()
        )
}
