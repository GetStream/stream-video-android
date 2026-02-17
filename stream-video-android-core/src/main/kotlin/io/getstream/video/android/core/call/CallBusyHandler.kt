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
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

internal class CallBusyHandler(private val streamVideo: StreamVideoClient) {

    fun rejectIfBusy(callId: StreamCallId, skipApiCall: Boolean = false): Boolean {
        val call = streamVideo.call(callId.type, callId.id)
        return rejectIfBusy(call, skipApiCall)
    }

    fun rejectIfBusy(call: Call, skipApiCall: Boolean = false): Boolean {
        val clientState = streamVideo.state

        if (!clientState.rejectCallWhenBusy) return false

        val activeCallId = clientState.activeCall.value?.id
        val ringingCallId = clientState.ringingCall.value?.id

        val isBusyWithAnotherCall =
            (activeCallId != null && activeCallId != call.id) ||
                (ringingCallId != null && ringingCallId != call.id)

        if (!isBusyWithAnotherCall) return false

        if (!skipApiCall) {
            //Actual Logic
            streamVideo.scope.launch {
                call.reject(RejectReason.Busy)
            }
        }

        return true
    }

    fun skipPropagateRingEvent(event: VideoEvent): Boolean {
        if (event is CallRingEvent) {
            val (type, id) = event.callCid.split(":")
            val call = streamVideo.call(type, id)
            return rejectIfBusy(call)
        }
        return true
    }
}
