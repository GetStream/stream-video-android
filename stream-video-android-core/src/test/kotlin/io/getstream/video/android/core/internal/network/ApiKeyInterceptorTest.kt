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