/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 *
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class CoordinatorSfuPinInterceptorTest {

    @Test
    fun `adds sfu_id on coordinator join when a pin is configured`() {
        val proceeded = intercept(
            pinnedSfuId = "SFU-1",
            url = "https://video.stream-io-api.com/video/call/default/abc/join?connection_id=c1",
        )

        assertThat(proceeded.url.queryParameter("sfu_id")).isEqualTo("SFU-1")
        assertThat(proceeded.url.queryParameter("connection_id")).isEqualTo("c1")
    }

    @Test
    fun `leaves non-join requests unchanged`() {
        val proceeded = intercept(
            pinnedSfuId = "SFU-1",
            url = "https://video.stream-io-api.com/video/call/default/abc",
        )

        assertThat(proceeded.url.queryParameter("sfu_id")).isNull()
    }

    @Test
    fun `leaves join unchanged when no pin is configured`() {
        val proceeded = intercept(
            pinnedSfuId = null,
            url = "https://video.stream-io-api.com/video/call/default/abc/join",
        )

        assertThat(proceeded.url.queryParameter("sfu_id")).isNull()
    }

    @Test
    fun `leaves join unchanged when the pin is blank`() {
        val proceeded = intercept(
            pinnedSfuId = "   ",
            url = "https://video.stream-io-api.com/video/call/default/abc/join",
        )

        assertThat(proceeded.url.queryParameter("sfu_id")).isNull()
    }

    @Test
    fun `does not duplicate an existing sfu_id`() {
        val proceeded = intercept(
            pinnedSfuId = "SFU-2",
            url = "https://video.stream-io-api.com/video/call/default/abc/join?sfu_id=SFU-1",
        )

        assertThat(proceeded.url.queryParameterValues("sfu_id")).containsExactly("SFU-1")
    }

    private fun intercept(pinnedSfuId: String?, url: String): Request {
        lateinit var proceeded: Request
        val client = OkHttpClient.Builder()
            .addInterceptor(CoordinatorSfuPinInterceptor(pinnedSfuId))
            .addInterceptor { chain ->
                proceeded = chain.request()
                Response.Builder()
                    .request(proceeded)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody())
                    .build()
            }
            .build()
        client.newCall(
            Request.Builder()
                .url(url)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute().close()
        return proceeded
    }
}
