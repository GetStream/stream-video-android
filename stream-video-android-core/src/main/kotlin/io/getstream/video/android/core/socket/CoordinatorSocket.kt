package io.getstream.video.android.core.socket

import com.squareup.moshi.JsonAdapter
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.User
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.ConnectUserDetailsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.WSAuthMessageRequest

/**
 * The Coordinator sockets send a user authentication request
 *  @see WSAuthMessageRequest
 *
 */
public class CoordinatorSocket(
    private val url: String,
    private val user: User,
    private val token: String,
    private val scope : CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    private val httpClient: OkHttpClient,
    private val networkStateProvider: NetworkStateProvider,
    ): PersistentSocket<ConnectedEvent>(
    url=url,
    httpClient=httpClient,
    scope = scope,
    token=token,
    networkStateProvider=networkStateProvider
) {
    override val logger by taggedLogger("PersistentCoordinatorSocket")


    override fun authenticate() {
        logger.d { "[authenticateUser] user: $user" }

        // TODO: handle guest and anon users

        if (token.isEmpty()) {
            throw IllegalStateException("User token is empty")
        }

        val adapter: JsonAdapter<WSAuthMessageRequest> =
            Serializer.moshi.adapter(WSAuthMessageRequest::class.java)

        val authRequest = WSAuthMessageRequest(
            token = token,
            userDetails = ConnectUserDetailsRequest(
                id = user.id,
                name = user.name,
                image = user.image,
            )
        )
        val message = adapter.toJson(authRequest)

        super.socket.send(message)
    }

}