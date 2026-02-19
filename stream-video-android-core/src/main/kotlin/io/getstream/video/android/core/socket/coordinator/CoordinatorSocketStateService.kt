/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.socket.coordinator

import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.socket.common.ConnectionConf
import io.getstream.video.android.core.socket.common.fsm.FiniteStateMachine
import io.getstream.video.android.core.socket.coordinator.state.RestartReason
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketConnectionType
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketStateEvent
import kotlinx.coroutines.flow.StateFlow

internal class CoordinatorSocketStateService(initialState: VideoSocketState = VideoSocketState.Disconnected.Stopped) {
    private val logger by taggedLogger("Video:SocketState")

    suspend fun observer(onNewState: suspend (VideoSocketState) -> Unit) {
        stateMachine.stateFlow.collect(onNewState)
    }

    /**
     * Require a reconnection.
     *
     * @param connectionConf The [VideoSocketFactory.ConnectionConf] to be used to reconnect.
     */
    suspend fun onReconnect(
        connectionConf: ConnectionConf,
        forceReconnection: Boolean,
    ) {
        logger.v {
            "[onReconnect] user.id: '${connectionConf.user.id}', isReconnection: ${connectionConf.isReconnection}"
        }
        stateMachine.sendEvent(
            VideoSocketStateEvent.Connect(
                connectionConf,
                when (forceReconnection) {
                    true -> VideoSocketConnectionType.FORCE_RECONNECTION
                    false -> VideoSocketConnectionType.AUTOMATIC_RECONNECTION
                },
            ),
        )
    }

    /**
     * Require connection.
     *
     * @param connectionConf The [VideoSocketFactory.ConnectionConf] to be used on the new connection.
     */
    suspend fun onConnect(connectionConf: ConnectionConf) {
        logger.v {
            "[onConnect] user.id: '${connectionConf.user.id}', isReconnection: ${connectionConf.isReconnection}"
        }
        stateMachine.sendEvent(
            VideoSocketStateEvent.Connect(
                connectionConf,
                VideoSocketConnectionType.INITIAL_CONNECTION,
            ),
        )
    }

    /**
     * Notify that the network is not available at the moment.
     */
    suspend fun onNetworkNotAvailable() {
        logger.w { "[onNetworkNotAvailable] no args" }
        stateMachine.sendEvent(VideoSocketStateEvent.NetworkNotAvailable)
    }

    /**
     * Notify the WebSocket connection has been established.
     *
     * @param connectedEvent The [ConnectedEvent] received within the WebSocket connection.
     */
    suspend fun onConnectionEstablished(connectedEvent: ConnectedEvent) {
        logger.i {
            "[onConnected] user.id: '${connectedEvent.me.id}', connectionId: ${connectedEvent.connectionId}"
        }
        stateMachine.sendEvent(VideoSocketStateEvent.ConnectionEstablished(connectedEvent))
    }

    /**
     * Notify that an unrecoverable error happened.
     *
     * @param error The [Error.NetworkError]
     */
    suspend fun onUnrecoverableError(error: Error.NetworkError) {
        logger.e { "[onUnrecoverableError] error: $error" }
        stateMachine.sendEvent(VideoSocketStateEvent.UnrecoverableError(error))
    }

    /**
     * Notify that a network error happened.
     *
     * @param error The [Error.NetworkError]
     */
    suspend fun onNetworkError(error: Error.NetworkError) {
        logger.e { "[onNetworkError] error: $error" }
        stateMachine.sendEvent(VideoSocketStateEvent.NetworkError(error))
    }

    /**
     * Notify that the user want to disconnect the WebSocket connection.
     */
    suspend fun onRequiredDisconnect() {
        logger.i { "[onRequiredDisconnect] no args" }
        stateMachine.sendEvent(VideoSocketStateEvent.RequiredDisconnection)
    }

    /**
     * Notify that the connection should be stopped.
     */
    suspend fun onStop() {
        logger.i { "[onStop] no args" }
        stateMachine.sendEvent(VideoSocketStateEvent.Stop)
    }

    /**
     * Notify that some WebSocket Event has been lost.
     */
    suspend fun onWebSocketEventLost() {
        logger.w { "[onWebSocketEventLost] no args" }
        stateMachine.sendEvent(VideoSocketStateEvent.WebSocketEventLost)
    }

    /**
     * Notify that the network is available at the moment.
     */
    suspend fun onNetworkAvailable() {
        logger.i { "[onNetworkAvailable] no args" }
        stateMachine.sendEvent(VideoSocketStateEvent.NetworkAvailable)
    }

    /**
     * Notify that the connection should be resumed.
     */
    suspend fun onResume() {
        logger.v { "[onResume] no args" }
        stateMachine.sendEvent(VideoSocketStateEvent.Resume)
    }

    /**
     * Current state of the WebSocket connection.
     */
    val currentState: VideoSocketState
        get() = stateMachine.state

    /**
     * Current state of the WebSocket connection as [StateFlow].
     */
    val currentStateFlow: StateFlow<VideoSocketState>
        get() = stateMachine.stateFlow

    private val stateMachine: FiniteStateMachine<VideoSocketState, VideoSocketStateEvent> by lazy {
        FiniteStateMachine {
            initialState(initialState)

            defaultHandler { state, event ->
                logger.e { "Cannot handle event $event while being in inappropriate state $state" }
                state
            }

            state<VideoSocketState.RestartConnection> {
                onEvent<VideoSocketStateEvent.Connect> {
                    VideoSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<VideoSocketStateEvent.ConnectionEstablished> {
                    VideoSocketState.Connected(it.connectedEvent)
                }
                onEvent<VideoSocketStateEvent.WebSocketEventLost> {
                    VideoSocketState.Disconnected.WebSocketEventLost
                }
                onEvent<VideoSocketStateEvent.NetworkNotAvailable> {
                    VideoSocketState.Disconnected.NetworkDisconnected
                }
                onEvent<VideoSocketStateEvent.UnrecoverableError> {
                    VideoSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<VideoSocketStateEvent.NetworkError> {
                    VideoSocketState.Disconnected.DisconnectedTemporarily(it.error)
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Stop> { VideoSocketState.Disconnected.Stopped }
            }

            state<VideoSocketState.Connecting> {
                onEvent<VideoSocketStateEvent.Connect> {
                    VideoSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<VideoSocketStateEvent.ConnectionEstablished> {
                    VideoSocketState.Connected(it.connectedEvent)
                }
                onEvent<VideoSocketStateEvent.WebSocketEventLost> {
                    VideoSocketState.Disconnected.WebSocketEventLost
                }
                onEvent<VideoSocketStateEvent.NetworkNotAvailable> {
                    VideoSocketState.Disconnected.NetworkDisconnected
                }
                onEvent<VideoSocketStateEvent.UnrecoverableError> {
                    VideoSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<VideoSocketStateEvent.NetworkError> {
                    VideoSocketState.Disconnected.DisconnectedTemporarily(it.error)
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Stop> { VideoSocketState.Disconnected.Stopped }
            }

            state<VideoSocketState.Connected> {
                onEvent<VideoSocketStateEvent.ConnectionEstablished> {
                    VideoSocketState.Connected(it.connectedEvent)
                }
                onEvent<VideoSocketStateEvent.WebSocketEventLost> {
                    VideoSocketState.Disconnected.WebSocketEventLost
                }
                onEvent<VideoSocketStateEvent.NetworkNotAvailable> {
                    VideoSocketState.Disconnected.NetworkDisconnected
                }
                onEvent<VideoSocketStateEvent.UnrecoverableError> {
                    VideoSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<VideoSocketStateEvent.NetworkError> {
                    VideoSocketState.Disconnected.DisconnectedTemporarily(it.error)
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Stop> { VideoSocketState.Disconnected.Stopped }
            }

            state<VideoSocketState.Disconnected.Stopped> {
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Connect> {
                    VideoSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<VideoSocketStateEvent.Resume> {
                    VideoSocketState.RestartConnection(
                        RestartReason.LIFECYCLE_RESUME,
                    )
                }
            }

            state<VideoSocketState.Disconnected.NetworkDisconnected> {
                onEvent<VideoSocketStateEvent.Connect> {
                    VideoSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<VideoSocketStateEvent.ConnectionEstablished> {
                    VideoSocketState.Connected(it.connectedEvent)
                }
                onEvent<VideoSocketStateEvent.UnrecoverableError> {
                    VideoSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<VideoSocketStateEvent.NetworkError> {
                    VideoSocketState.Disconnected.DisconnectedTemporarily(it.error)
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Stop> { VideoSocketState.Disconnected.Stopped }
                onEvent<VideoSocketStateEvent.NetworkAvailable> {
                    VideoSocketState.RestartConnection(
                        RestartReason.NETWORK_AVAILABLE,
                    )
                }
            }

            state<VideoSocketState.Disconnected.WebSocketEventLost> {
                onEvent<VideoSocketStateEvent.Connect> {
                    VideoSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<VideoSocketStateEvent.ConnectionEstablished> {
                    VideoSocketState.Connected(it.connectedEvent)
                }
                onEvent<VideoSocketStateEvent.NetworkNotAvailable> {
                    VideoSocketState.Disconnected.NetworkDisconnected
                }
                onEvent<VideoSocketStateEvent.UnrecoverableError> {
                    VideoSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<VideoSocketStateEvent.NetworkError> {
                    VideoSocketState.Disconnected.DisconnectedTemporarily(it.error)
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Stop> { VideoSocketState.Disconnected.Stopped }
            }

            state<VideoSocketState.Disconnected.DisconnectedByRequest> {
                onEvent<VideoSocketStateEvent.RequiredDisconnection> { currentState }
                onEvent<VideoSocketStateEvent.Connect> {
                    when (it.connectionType) {
                        VideoSocketConnectionType.INITIAL_CONNECTION -> VideoSocketState.Connecting(
                            it.connectionConf,
                            it.connectionType,
                        )
                        VideoSocketConnectionType.AUTOMATIC_RECONNECTION -> this
                        VideoSocketConnectionType.FORCE_RECONNECTION -> VideoSocketState.Connecting(
                            it.connectionConf,
                            it.connectionType,
                        )
                    }
                }
            }

            state<VideoSocketState.Disconnected.DisconnectedTemporarily> {
                onEvent<VideoSocketStateEvent.Connect> {
                    VideoSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<VideoSocketStateEvent.ConnectionEstablished> {
                    VideoSocketState.Connected(it.connectedEvent)
                }
                onEvent<VideoSocketStateEvent.NetworkNotAvailable> {
                    VideoSocketState.Disconnected.NetworkDisconnected
                }
                onEvent<VideoSocketStateEvent.WebSocketEventLost> {
                    VideoSocketState.Disconnected.WebSocketEventLost
                }
                onEvent<VideoSocketStateEvent.UnrecoverableError> {
                    VideoSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<VideoSocketStateEvent.NetworkError> {
                    VideoSocketState.Disconnected.DisconnectedTemporarily(it.error)
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<VideoSocketStateEvent.Stop> { VideoSocketState.Disconnected.Stopped }
            }

            state<VideoSocketState.Disconnected.DisconnectedPermanently> {
                onEvent<VideoSocketStateEvent.Connect> {
                    when (it.connectionType) {
                        VideoSocketConnectionType.INITIAL_CONNECTION -> VideoSocketState.Connecting(
                            it.connectionConf,
                            it.connectionType,
                        )
                        VideoSocketConnectionType.AUTOMATIC_RECONNECTION -> this
                        VideoSocketConnectionType.FORCE_RECONNECTION -> VideoSocketState.Connecting(
                            it.connectionConf,
                            it.connectionType,
                        )
                    }
                }
                onEvent<VideoSocketStateEvent.RequiredDisconnection> {
                    VideoSocketState.Disconnected.DisconnectedByRequest
                }
            }
        }
    }
}
