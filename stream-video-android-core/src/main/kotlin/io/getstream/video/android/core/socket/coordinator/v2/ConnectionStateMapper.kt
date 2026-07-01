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

package io.getstream.video.android.core.socket.coordinator.v2

import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.result.Error
import io.getstream.video.android.core.ConnectionState

/**
 * Maps a core [StreamConnectionState] onto the SDK's existing [ConnectionState] shape.
 *
 * The mapping produced here is a placeholder that preserves compilation until a later plan
 * reshapes the video [ConnectionState] sealed interface to mirror the richer core states 1:1
 * (D-04, D-09). Once that reshape lands, both this function body and the collaborating tests
 * flip together in a single commit.
 */
internal fun StreamConnectionState.toVideoConnectionState(): ConnectionState {
    // TODO(02-03): replace placeholder mapping when ConnectionState sealed interface is
    // rewritten per D-04/D-09.
    return when (this) {
        is StreamConnectionState.Idle -> ConnectionState.PreConnect
        is StreamConnectionState.Connecting.Opening -> ConnectionState.Loading
        is StreamConnectionState.Connecting.Authenticating -> ConnectionState.Loading
        is StreamConnectionState.Connected -> ConnectionState.Connected
        is StreamConnectionState.Disconnected -> {
            val throwable = cause
            if (throwable == null) {
                ConnectionState.Disconnected
            } else {
                ConnectionState.Failed(
                    Error.ThrowableError(
                        message = throwable.message ?: "",
                        cause = throwable,
                    ),
                )
            }
        }
    }
}
