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

package io.getstream.video.android.core.slowevent

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.events.CallRejectedSlowEvent

internal class CallRejectedSlowEventDetector : SlowEventDetector {
    private val logger by taggedLogger("CallRejectedSlowEventDetector")

    override fun detectAndMarkSlowEvent(call: Call, slowEventContext: SlowEventContext): Boolean {
        val ringingState = call.state.ringingState.value
        if (ringingState is RingingState.Incoming) {
            if (!ringingState.acceptedByMe) {
                /**
                 * Using dispatcher in notification can cause 15 seconds delay
                 * One alternative is to use this inside service class
                 */
                logger.d { "[onMissedCall] Ringing State: $ringingState" }
                call.state.updateSlowEvent(CallRejectedSlowEvent())
                return true
            }
        }
        return false
    }
}
