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

package io.getstream.video.android.core.socket.sfu

import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.socket.common.ConnectionConf
import io.getstream.video.android.core.socket.common.fsm.FiniteStateMachine
import io.getstream.video.android.core.socket.sfu.state.RestartReason
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.core.socket.sfu.state.SfuSocketStateEvent
import kotlinx.coroutines.flow.StateFlow
import org.openapitools.client.models.ConnectedEvent
import stream.video.sfu.models.WebsocketReconnectStrategy

internal class SfuSocketStateService(initialState: SfuSocketState = SfuSocketState.Disconnected.Stopped) {

    private val logger by taggedLogger("Video:SfuSocketState")

    suspend fun observer(onNewState: suspend (SfuSocketState) -> Unit) {
        stateMachine.stateFlow.collect(onNewState)
    }

    /**
     * Require a reconnection.
     *
     * @param connectionConf The [SocketFactory.ConnectionConf] to be used to reconnect.
     */
    suspend fun onReconnect(
        connectionConf: ConnectionConf.SfuConnectionConf,
    ) {
        logger.v {
            "[onReconnect] user.id: '${connectionConf.user.id}', isReconnection: ${connectionConf.isReconnection}"
        }
        stateMachine.sendEvent(
            SfuSocketStateEvent.Connect(
                connectionConf,
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            ),
        )
    }

    /**
     * Require connection.
     *
     * @param connectionConf The [VideoSocketFactory.ConnectionConf] to be used on the new connection.
     */
    suspend fun onConnect(connectionConf: ConnectionConf.SfuConnectionConf) {
        logger.v {
            "[onConnect] user.id: '${connectionConf.user.id}', isReconnection: ${connectionConf.isReconnection}"
        }
        stateMachine.sendEvent(
            SfuSocketStateEvent.Connect(
                connectionConf,
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
            ),
        )
    }

    /**
     * Notify that the network is not available at the moment.
     */
    suspend fun onNetworkNotAvailable() {
        logger.w { "[onNetworkNotAvailable] no args" }
        stateMachine.sendEvent(SfuSocketStateEvent.NetworkNotAvailable)
    }

    /**
     * Notify the WebSocket connection has been established.
     *
     * @param connectedEvent The [ConnectedEvent] received within the WebSocket connection.
     */
    suspend fun onConnectionEstablished(connectedEvent: JoinCallResponseEvent) {
        logger.i {
            "[onConnected] client.id: '$connectedEvent'"
        }
        stateMachine.sendEvent(SfuSocketStateEvent.ConnectionEstablished(connectedEvent))
    }

    /**
     * Notify that an unrecoverable error happened.
     *
     * @param error The [Error.NetworkError]
     */
    suspend fun onUnrecoverableError(error: Error.NetworkError) {
        logger.e { "[onUnrecoverableError] error: $error" }
        stateMachine.sendEvent(SfuSocketStateEvent.UnrecoverableError(error))
    }

    /**
     * Notify that a network error happened.
     *
     * @param error The [Error.NetworkError]
     */
    suspend fun onNetworkError(
        error: Error.NetworkError,
        reconnectStrategy: WebsocketReconnectStrategy,
    ) {
        logger.e { "[onNetworkError] error: $error" }
        stateMachine.sendEvent(SfuSocketStateEvent.NetworkError(error, reconnectStrategy))
    }

    /**
     * Notify that the user want to disconnect the WebSocket connection.
     */
    suspend fun onRequiredDisconnect() {
        logger.i { "[onRequiredDisconnect] no args" }
        stateMachine.sendEvent(SfuSocketStateEvent.RequiredDisconnection)
    }

    /**
     * Notify that the connection should be stopped.
     */
    suspend fun onStop() {
        logger.i { "[onStop] no args" }
        stateMachine.sendEvent(SfuSocketStateEvent.Stop)
    }

    /**
     * Notify that some WebSocket Event has been lost.
     */
    suspend fun onWebSocketEventLost() {
        logger.w { "[onWebSocketEventLost] no args" }
        stateMachine.sendEvent(SfuSocketStateEvent.WebSocketEventLost)
    }

    /**
     * Notify that the network is available at the moment.
     */
    suspend fun onNetworkAvailable() {
        logger.i { "[onNetworkAvailable] no args" }
        stateMachine.sendEvent(SfuSocketStateEvent.NetworkAvailable)
    }

    /**
     * Notify that the connection should be resumed.
     */
    suspend fun onResume() {
        logger.v { "[onResume] no args" }
        stateMachine.sendEvent(SfuSocketStateEvent.Resume)
    }

    /**
     * Current state of the WebSocket connection.
     */
    val currentState: SfuSocketState
        get() = stateMachine.state

    /**
     * Current state of the WebSocket connection as [StateFlow].
     */
    val currentStateFlow: StateFlow<SfuSocketState>
        get() = stateMachine.stateFlow

    private val stateMachine: FiniteStateMachine<SfuSocketState, SfuSocketStateEvent> by lazy {
        FiniteStateMachine {
            initialState(initialState)

            defaultHandler { state, event ->
                logger.e { "Cannot handle event $event while being in inappropriate state $state" }
                state
            }

            state<SfuSocketState.RestartConnection> {
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<SfuSocketStateEvent.ConnectionEstablished> {
                    SfuSocketState.Connected(
                        it.connectedEvent,
                    )
                }
                onEvent<SfuSocketStateEvent.WebSocketEventLost> { SfuSocketState.Disconnected.WebSocketEventLost }
                onEvent<SfuSocketStateEvent.NetworkNotAvailable> { SfuSocketState.Disconnected.NetworkDisconnected }
                onEvent<SfuSocketStateEvent.UnrecoverableError> {
                    SfuSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkError> {
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        it.error,
                        it.reconnectStrategy,
                    )
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Stop> { SfuSocketState.Disconnected.Stopped }
            }

            state<SfuSocketState.Connecting> {
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<SfuSocketStateEvent.ConnectionEstablished> {
                    SfuSocketState.Connected(
                        it.connectedEvent,
                    )
                }
                onEvent<SfuSocketStateEvent.WebSocketEventLost> { SfuSocketState.Disconnected.WebSocketEventLost }
                onEvent<SfuSocketStateEvent.NetworkNotAvailable> { SfuSocketState.Disconnected.NetworkDisconnected }
                onEvent<SfuSocketStateEvent.UnrecoverableError> {
                    SfuSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkError> {
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        it.error,
                        it.reconnectStrategy,
                    )
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Stop> { SfuSocketState.Disconnected.Stopped }
            }

            state<SfuSocketState.Connected> {
                onEvent<SfuSocketStateEvent.ConnectionEstablished> {
                    SfuSocketState.Connected(
                        it.connectedEvent,
                    )
                }
                onEvent<SfuSocketStateEvent.WebSocketEventLost> { SfuSocketState.Disconnected.WebSocketEventLost }
                onEvent<SfuSocketStateEvent.NetworkNotAvailable> { SfuSocketState.Disconnected.NetworkDisconnected }
                onEvent<SfuSocketStateEvent.UnrecoverableError> {
                    SfuSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkError> {
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        it.error,
                        it.reconnectStrategy,
                    )
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Stop> { SfuSocketState.Disconnected.Stopped }
            }

            state<SfuSocketState.Disconnected.Stopped> {
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
            }

            state<SfuSocketState.Disconnected.NetworkDisconnected> {
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<SfuSocketStateEvent.ConnectionEstablished> {
                    SfuSocketState.Connected(
                        it.connectedEvent,
                    )
                }
                onEvent<SfuSocketStateEvent.UnrecoverableError> {
                    SfuSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkError> {
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        it.error,
                        it.reconnectStrategy,
                    )
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Stop> { SfuSocketState.Disconnected.Stopped }
                onEvent<SfuSocketStateEvent.NetworkAvailable> {
                    SfuSocketState.RestartConnection(
                        RestartReason.NETWORK_AVAILABLE,
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                    )
                }
            }

            state<SfuSocketState.Disconnected.WebSocketEventLost> {
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<SfuSocketStateEvent.ConnectionEstablished> {
                    SfuSocketState.Connected(
                        it.connectedEvent,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkNotAvailable> { SfuSocketState.Disconnected.NetworkDisconnected }
                onEvent<SfuSocketStateEvent.UnrecoverableError> {
                    SfuSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkError> {
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        it.error,
                        it.reconnectStrategy,
                    )
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Stop> { SfuSocketState.Disconnected.Stopped }
            }

            state<SfuSocketState.Disconnected.DisconnectedByRequest> {
                onEvent<SfuSocketStateEvent.RequiredDisconnection> { currentState }
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(
                        it.connectionConf,
                        it.connectionType,
                    )
                }
            }

            state<SfuSocketState.Disconnected.DisconnectedTemporarily> {
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<SfuSocketStateEvent.ConnectionEstablished> {
                    SfuSocketState.Connected(
                        it.connectedEvent,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkNotAvailable> { SfuSocketState.Disconnected.NetworkDisconnected }
                onEvent<SfuSocketStateEvent.WebSocketEventLost> { SfuSocketState.Disconnected.WebSocketEventLost }
                onEvent<SfuSocketStateEvent.UnrecoverableError> {
                    SfuSocketState.Disconnected.DisconnectedPermanently(
                        it.error,
                    )
                }
                onEvent<SfuSocketStateEvent.NetworkError> {
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        it.error,
                        it.reconnectStrategy,
                    )
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
                onEvent<SfuSocketStateEvent.Stop> { SfuSocketState.Disconnected.Stopped }
            }

            state<SfuSocketState.Disconnected.DisconnectedPermanently> {
                onEvent<SfuSocketStateEvent.Connect> {
                    SfuSocketState.Connecting(it.connectionConf, it.connectionType)
                }
                onEvent<SfuSocketStateEvent.RequiredDisconnection> {
                    SfuSocketState.Disconnected.DisconnectedByRequest
                }
            }
        }
    }
}
