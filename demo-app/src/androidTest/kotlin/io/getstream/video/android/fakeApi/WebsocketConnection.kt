package io.getstream.video.android.fakeApi

import com.appunite.mockwebserverextensions.waitForCondition
import okhttp3.WebSocket
import org.intellij.lang.annotations.Language

class ConnectionSimulator {

    var activeConnection: WebSocketConnection? = null

    fun tearDown() {
        activeConnection = null
    }

    fun sendMessage(@Language("JSON") json: String) {
        activeConnection?.sendMessage(json)
    }


    fun onWebsocketConnected(connection: WebSocketConnection) {
        connection.sendMessage("""
           {"connection_id":"66ec3d5a-0a05-4c41-0200-0000041555d3","type":"health.check","created_at":"2024-10-29T20:28:15.010878537Z"}
        """.trimIndent())
    }

    fun waitForConnection() {
        waitForCondition(timeoutMessage = "Waiting for websocket connection timeout") { activeConnection != null}
    }

    fun waitForDisconnection() {
        waitForCondition(
            timeoutMillis = 10_000L,
            timeoutMessage = "By default KC has 30 seconds connection timeout. If this fail consider settings WebSocket.connectionCachingTimeForTestSeconds = 5L in set-up in your test"
        ) { activeConnection == null }
    }
}

class WebSocketConnection(val webSocket: WebSocket) {
    fun sendMessage(message: String) {
            webSocket.send(message)
    }
}