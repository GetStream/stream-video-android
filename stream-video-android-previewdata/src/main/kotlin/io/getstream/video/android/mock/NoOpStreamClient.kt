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

package io.getstream.video.android.mock

import io.getstream.android.core.api.StreamClient
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.android.core.api.socket.listeners.StreamClientListener
import io.getstream.android.core.api.subscribe.StreamSubscription
import io.getstream.android.core.api.subscribe.StreamSubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A no-op [StreamClient] used by preview/snapshot harnesses. Paparazzi runs under layoutlib's
 * [android.content.Context] which does not implement system services like WifiManager, so the
 * real client's factory would throw during initialisation. This fake side-steps that by holding
 * only an [StreamConnectionState.Idle] state flow and returning inert subscriptions.
 */
internal object NoOpStreamClient : StreamClient {

    override val connectionState: StateFlow<StreamConnectionState> =
        MutableStateFlow(StreamConnectionState.Idle)

    override fun subscribe(
        listener: StreamClientListener,
        options: StreamSubscriptionManager.Options,
    ): Result<StreamSubscription> = Result.success(NoOpSubscription)

    override suspend fun connect(): Result<io.getstream.android.core.api.model.connection.StreamConnectedUser> =
        Result.failure(UnsupportedOperationException("NoOpStreamClient does not connect"))

    override suspend fun disconnect(): Result<Unit> = Result.success(Unit)

    private object NoOpSubscription : StreamSubscription {
        override fun cancel() = Unit
    }
}
