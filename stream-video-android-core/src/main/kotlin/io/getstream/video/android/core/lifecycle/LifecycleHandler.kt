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

package io.getstream.video.android.core.lifecycle

import io.getstream.log.StreamLog
import io.getstream.video.android.core.socket.common.SocketConnectionPolicy
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketStateService

internal interface LifecycleHandler {
    suspend fun resume()
    suspend fun stopped()
}

internal class ConnectionPolicyLifecycleHandler(
    private val connectionPolicies: List<SocketConnectionPolicy>,
    private val socketStateService: CoordinatorSocketStateService,
) : LifecycleHandler {

    override suspend fun resume() {
        val shouldConnect = connectionPolicies.all { it.shouldConnect() }

        StreamLog.d(TAG) { "[resume] shouldConnect: $shouldConnect" }

        if (shouldConnect) socketStateService.onResume()
    }

    override suspend fun stopped() {
        val shouldDisconnect = connectionPolicies.all { it.shouldDisconnect() }

        StreamLog.d(TAG) { "[stopped] shouldDisconnect: $shouldDisconnect" }

        if (shouldDisconnect) socketStateService.onStop()
    }
}

internal class NoOpLifecycleHandler : LifecycleHandler {
    override suspend fun resume() { /* No-op */ }
    override suspend fun stopped() { /* No-op */ }
}

private const val TAG = "Video:LifecycleHandler"
