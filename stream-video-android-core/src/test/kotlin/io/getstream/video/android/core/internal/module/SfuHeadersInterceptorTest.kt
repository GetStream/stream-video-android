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

package io.getstream.video.android.core.internal.module

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.header.HeadersUtil
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class SfuHeadersInterceptorTest {

    private val twirpUrl = "https://sfu.example.com/twirp/signal.SignalServer/SendStats"

    @Test
    fun `identifies the SDK and its version on every twirp request`() {
        val request = sendTwirpRequest()

        assertThat(request.header("X-Stream-Client"))
            .isEqualTo("stream-android@${BuildConfig.STREAM_VIDEO_VERSION}")
    }

    @Test
    fun `sends a single client header`() {
        val request = sendTwirpRequest()

        assertThat(request.headers("X-Stream-Client")).hasSize(1)
    }

    private fun sendTwirpRequest(): Request {
        lateinit var sent: Request
        val client = OkHttpClient.Builder()
            .addInterceptor(SfuHeadersInterceptor(HeadersUtil()))
            .addInterceptor(
                Interceptor { chain ->
                    sent = chain.request()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody())
                        .build()
                },
            )
            .build()

        client.newCall(Request.Builder().url(twirpUrl).build()).execute().close()
        return sent
    }
}
