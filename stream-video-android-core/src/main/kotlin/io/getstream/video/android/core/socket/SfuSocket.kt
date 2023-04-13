package io.getstream.video.android.core.socket

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.internal.network.NetworkStateProvider
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
    private val httpClient: OkHttpClient,
    private val sessionId: String,
    private val token: String,
    private val getSubscriberSdp: () -> String,
    private val networkStateProvider: NetworkStateProvider
): PersistentSocket(
    url=url,
    httpClient=httpClient,
    token=token,
    networkStateProvider=networkStateProvider
) {

    override val logger by taggedLogger("PersistentSFUSocket")

    override fun authenticate() {
        logger.d { "[authenticate] sessionId: $sessionId" }
        val request = JoinRequest(
            session_id = sessionId,
            token = token,
            subscriber_sdp = getSubscriberSdp()
        )
        socket.send(SfuRequest(join_request = request).encodeByteString())

    }

}