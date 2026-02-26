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

package io.getstream.video.android.core.call

import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CallBusyHandler(
    private val rejectCallWhenBusy: Boolean,
    private val activeCall: StateFlow<Call?>,
    private val ringingCall: StateFlow<Call?>,
) {

    private val _callBusyHandlerState: MutableStateFlow<CallBusyHandlerState?> =
        MutableStateFlow(null)
    internal val callBusyHandlerState: StateFlow<CallBusyHandlerState?> = _callBusyHandlerState
    fun shouldPropagateEvent(event: CallRingEvent): Boolean {
        return !isBusyWithAnotherCall(event.callCid, CallBusyHandlerCheckerSource.VIDEO_CLIENT)
    }

    fun isBusyWithAnotherCall(callCid: String, source: CallBusyHandlerCheckerSource = CallBusyHandlerCheckerSource.VIDEO_CLIENT): Boolean {
        if (!rejectCallWhenBusy) return false

        val streamCallId = StreamCallId.fromCallCid(callCid)
        val isDifferentFromActiveCall = if (activeCall.value != null) {
            activeCall.value?.cid != streamCallId.cid
        } else {
            false
        }

        val isDifferentFromRingingCall = if (ringingCall.value != null) {
            ringingCall.value?.cid != streamCallId.cid
        } else {
            false
        }
        val isBusyWithAnotherCall = isDifferentFromActiveCall || isDifferentFromRingingCall

        if (isBusyWithAnotherCall) {
            _callBusyHandlerState.value = CallBusyHandlerState(streamCallId, source)
        }

        return isBusyWithAnotherCall
    }

    internal enum class CallBusyHandlerCheckerSource {
        NOTIFICATION, VIDEO_CLIENT
    }

    internal data class CallBusyHandlerState(val streamCallId: StreamCallId, val source: CallBusyHandlerCheckerSource)
}
