/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.socket

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.SfuRequest

/**
 * The SFU socket is slightly different from the coordinator socket
 * It sends a JoinRequest to authenticate
 * SFU socket uses binary instead of text
 */
public class SfuSocket(
    private val url: String,
    private val sessionId: String,
    private val token: String,
    private val getSubscriberSdp: suspend () -> String,
    private val scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    private val httpClient: OkHttpClient,
    private val networkStateProvider: NetworkStateProvider
) : PersistentSocket<JoinCallResponseEvent> (
    url = url,
    httpClient = httpClient,
    scope = scope,
    networkStateProvider = networkStateProvider
) {

    override val logger by taggedLogger("PersistentSFUSocket")

    override fun authenticate() {
        logger.d { "[authenticate] sessionId: $sessionId" }
        scope.launch {
            val request = JoinRequest(
                session_id = sessionId,
                token = token,
                subscriber_sdp = getSubscriberSdp()
            )
            socket?.send(SfuRequest(join_request = request).encodeByteString())
        }
    }
}
