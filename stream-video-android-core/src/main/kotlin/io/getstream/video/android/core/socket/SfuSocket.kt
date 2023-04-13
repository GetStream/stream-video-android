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
    private val scope : CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    private val httpClient: OkHttpClient,
    private val networkStateProvider: NetworkStateProvider
): PersistentSocket<JoinCallResponseEvent> (
    url=url,
    httpClient=httpClient,
    token=token,
    scope = scope,
    networkStateProvider=networkStateProvider
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
            socket.send(SfuRequest(join_request = request).encodeByteString())
        }
    }

}