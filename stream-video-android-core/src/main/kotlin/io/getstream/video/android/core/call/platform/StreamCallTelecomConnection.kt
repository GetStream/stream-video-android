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

package io.getstream.video.android.core.call.platform

import android.telecom.Connection
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A connection used to manage the call via the telecom API.
 *
 * @param call the call that manages this connection
 * @param scope the coroutine scope.
 */
internal class StreamCallTelecomConnection(
    val call: Call,
    val scope: CoroutineScope = CoroutineScope(
        Dispatchers.Default,
    ),
) : Connection() {

    companion object {
        val logger by taggedLogger("StreamTelecomConnection")
    }

    override fun onAnswer() {
        logger.d { "[telecom] onAnswer[${call.cid}]" }
        scope.launch {
            call.join()
        }
        super.onAnswer()
    }

    override fun onDisconnect() {
        logger.d { "[telecom] onDisconnect[${call.cid}]" }
        call.leave()
        super.onDisconnect()
    }

    override fun onHold() {
        logger.d { "[telecom] onHold[${call.cid}]" }
        call.microphone.pause()
        call.camera.pause()
        super.onHold()
    }

    override fun onUnhold() {
        logger.d { "[telecom] onUnhold[${call.cid}]" }
        scope.launch {
            call.microphone.resume()
            call.camera.resume()
        }
        super.onUnhold()
    }

    override fun onAbort() {
        logger.d { "[telecom] onAbort[${call.cid}]" }
        call.leave()
        super.onAbort()
    }

    override fun onReject() {
        logger.d { "[telecom] onReject[${call.cid}]" }
        scope.launch {
            call.reject()
        }
        super.onReject()
    }
}
