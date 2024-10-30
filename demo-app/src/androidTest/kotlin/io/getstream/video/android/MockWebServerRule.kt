package io.getstream.video.android

import android.util.Log
import com.appunite.mockwebserver_interceptor.TestInterceptor
import com.appunite.mockwebserverextensions.MockWebServerRule
import com.appunite.mockwebserverextensions.intercept.UrlOverrideInterceptor
import io.getstream.video.android.fakeApi.FakeGetStreamApi
import io.getstream.video.android.fakeApi.registerGetStreamApi
import io.getstream.video.android.util.StreamVideoInitHelper
import org.junit.runner.Description

class StreamMockWebServerRule(registerDefaults: Boolean = true) : MockWebServerRule() {

    val fakeGetStreamApi = FakeGetStreamApi()

    init {
        if (registerDefaults) {
            registerGetStreamApi(fakeGetStreamApi)
        }
    }
    override fun starting(description: Description?) {
        super.starting(description)

        val callWebsocket = server.url("/").let { "${it.host}:${it.port}/call/websocket" }
        fakeGetStreamApi.localhostWebsocketUrl = callWebsocket
        Log.i("MockWEbServer", "Server url: $callWebsocket")

        StreamVideoInitHelper.testUrl = server.url("/hint").toString()
        TestInterceptor.testInterceptor = UrlOverrideInterceptor(server.url("/"))
    }

    override fun finished(description: Description) {
        TestInterceptor.testInterceptor = null
        super.finished(description)
    }
}
