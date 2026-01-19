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

package io.getstream.video.android.core.socket.common

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.header.HeadersUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.UnsupportedEncodingException

internal class SocketFactory<V, P : GenericParser<V>, C : ConnectionConf>(
    private val parser: P,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val logger by taggedLogger("Video:SocketFactory")
    private val headersUtil = HeadersUtil()

    @Throws(UnsupportedEncodingException::class)
    fun <T : VideoEvent> createSocket(connectionConf: C, tag: String): StreamWebSocket<V, P> {
        val request = buildRequest(connectionConf)
        logger.i { "[createSocket] new web socket: ${request.url}" }
        return StreamWebSocket(tag, parser) { httpClient.newWebSocket(request, it) }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun buildRequest(connectionConf: C): Request =
        Request.Builder()
            .url(connectionConf.endpoint)
            .addHeader("Connection", "Upgrade")
            .addHeader("Upgrade", "websocket")
            .addHeader("X-Stream-Client", headersUtil.buildSdkTrackingHeaders())
            .build()
}
