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

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.launch

internal class CallBusyHandler(private val streamVideo: StreamVideoClient) {

    fun rejectIfBusy(callId: StreamCallId): Boolean {
        val call = streamVideo.call(callId.type, callId.id)
        return rejectIfBusy(call)
    }
    fun rejectIfBusy(call: Call): Boolean {
        val state = streamVideo.state

        if (!state.rejectCallWhenBusy) return false
        if (!state.hasActiveOrRingingCall()) return false

        streamVideo.scope.launch { call.reject(RejectReason.Busy) }
        return true
    }
}
