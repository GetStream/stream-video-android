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

package io.getstream.video.android.core.internal.network

import io.getstream.chat.android.client.api.FakeResponse
import io.getstream.video.android.core.randomString
import org.junit.Assert
import org.junit.Test

class ApiKeyInterceptorTest {

    @Test
    fun testApiKeyIsAddedAsHeader() {
        // given
        val apiKey = randomString()
        val interceptor = ApiKeyInterceptor(apiKey)
        // when
        val response = interceptor.intercept(FakeChain(FakeResponse(200)))
        // then
        val apiKeyQueryParam = response.request.url.queryParameter("api_key")
        Assert.assertEquals(apiKey, apiKeyQueryParam)
    }
}
