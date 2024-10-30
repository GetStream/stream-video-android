package io.getstream.video.android.fakeApi

import android.util.Log
import com.appunite.mockwebserver_assertions.method
import com.appunite.mockwebserver_assertions.path
import com.appunite.mockwebserver_assertions.url
import com.appunite.mockwebserverextensions.MockRegistry
import com.appunite.mockwebserverextensions.util.jsonResponse
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.matches

class FakeGetStreamApi {
    var localhostWebsocketUrl: String = ""

    private val connectionSimulator = ConnectionSimulator()
    private val callConnectionSimulator = ConnectionSimulator()

    fun onWebsocketConnected(connection: WebSocketConnection) {
        connectionSimulator.onWebsocketConnected(connection)
    }

    fun setActiveConnectionNull() {
        connectionSimulator.activeConnection = null
    }

    fun onCallConnected(connection: WebSocketConnection) {
        callConnectionSimulator.onWebsocketConnected(connection)
    }

    fun setCallConnectionNull() {
        callConnectionSimulator.activeConnection = null
    }

    fun onClientMessage(text: String) {
        Log.i("MockWebServer Connection", "[on client message] $text")
    }

    fun onCallMessage(text: String) {
        Log.i("MockWebServer Call Connection", "[on client message] $text")
    }
}

fun MockRegistry.registerGetStreamApi(fakeGetStreamApi: FakeGetStreamApi) {
    register {
        expectThat(it) {
            method.isEqualTo("POST")
            url.path.isEqualTo("/video/devices")
        }

        jsonResponse("""{ "duration" : "1ms" }""".trimIndent())
    }

    register {
        expectThat(it) {
            method.isEqualTo("POST")
            url.path.matches(Regex("/video/call/default/[^/]+"))
        }

        callCreateResponse
    }

    register {
        expectThat(it) {
            method.isEqualTo("POST")
            url.path.matches(Regex("/video/call/default/[^/]+/join"))
        }

        callJoinResponse(fakeGetStreamApi.localhostWebsocketUrl)
    }

    register {
        expectThat(it) {
            method.isEqualTo("HEAD")
            url.path.isEqualTo("/hint")
        }

        MockResponse().setResponseCode(200)
    }
    register {
        expectThat(it) {
            method.isEqualTo("GET")
            url.path.isEqualTo("/call/websocket")
        }

        MockResponse().withWebSocketUpgrade(object : WebSocketListener() {

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                fakeGetStreamApi.setCallConnectionNull()
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                fakeGetStreamApi.onCallConnected(WebSocketConnection(webSocket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                fakeGetStreamApi.onCallMessage(text)
            }

        })
    }
    register {
        expectThat(it) {
            method.isEqualTo("GET")
            url.path.isEqualTo("/video/connect")
        }

        MockResponse().withWebSocketUpgrade(object : WebSocketListener() {

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                fakeGetStreamApi.setActiveConnectionNull()
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                fakeGetStreamApi.onWebsocketConnected(WebSocketConnection(webSocket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                fakeGetStreamApi.onClientMessage(text)
            }

        })
    }
}