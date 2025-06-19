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

package io.getstream.video.android.core.internal

import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.model.Credentials
import io.getstream.video.android.core.trace.TracerManager
import io.getstream.video.android.model.ApiKey
import kotlinx.coroutines.CoroutineScope

internal class RtcSessionFactory(
    private val lifecycle: Lifecycle,
    private val apiKey: ApiKey,
    private val powerManager: PowerManager,
    private val tracerManager: TracerManager,
    private val coroutineScope: CoroutineScope,
) {

    fun create(
        credentials: Credentials,
        streamVideo: StreamVideo,
        call: Call,
        sessionId: String,
    ): RtcSession =
        RtcSession(
            client = streamVideo,
            call = call,
            lifecycle = lifecycle,
            apiKey = apiKey,
            sfuUrl = credentials.server.url,
            sfuWsUrl = credentials.server.wsUrl,
            sfuToken = credentials.token,
            remoteIceServers = credentials.iceServers,
            powerManager = powerManager,
            coroutineScope = coroutineScope,
            sessionId = sessionId,
            tracerManager = tracerManager,
        )
}
