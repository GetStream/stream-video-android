package io.getstream.video.android.client.api

import io.getstream.android.push.PushDevice
import io.getstream.video.android.client.api.listeners.StreamVideoClientListener
import io.getstream.video.android.client.api.state.StreamVideoClientState
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.api.state.connection.StreamConnectionRetryConfig
import io.getstream.video.android.client.api.state.connection.StreamVideoClientConnectionState
import io.getstream.video.android.client.model.ConnectUserData

/**
 * The main interface to control the Video calls.
 */
public interface StreamVideoClient {

    /**
     * Create a call with the given type and id.
     */
    public fun call(type: String, id: String): Result<StreamCall>

    /**
     * Connect the user to the socket. By specifying a retry config you can retry the connection
     * in case of certain recoverable API errors.
     *
     * @param data The connect data to use for the coordinator.
     * @param retry The retry configuration for the connection.
     */
    public suspend fun connect(
        data: ConnectUserData,
        retry: StreamConnectionRetryConfig = StreamConnectionRetryConfig()
    ): StreamVideoClientConnectionState

    /**
     * Register a device for push notifications.
     */
    public suspend fun registerDevice(pushDevice: PushDevice): Result<PushDevice>

    /**
     * Unregister a device for push notifications.
     */
    public suspend fun unregisterDevice(token: String): Result<Unit>

    /**
     * Disconnect the user.
     */
    public suspend fun disconnect(): Result<Unit>

    /**
     * Get the state of the client.
     */
    public fun getState(): Result<StreamVideoClientState>

    /**
     * Subscribe for client events and state updates.
     *
     * @param listener The listener to subscribe.
     */
    public fun subscribe(listener: StreamVideoClientListener): Result<StreamSubscription>
}