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

package io.getstream.video.android.core.call

/**
 * Typed SFU connect failure produced by [RtcSession.connectInternal].
 */
internal sealed interface SfuConnectFailure {
    val error: Exception

    /**
     * [RtcSession.connectInternal] stopped waiting for the socket to reach a terminal state.
     */
    data class SocketStateObservationTimeout(
        override val error: Exception,
    ) : SfuConnectFailure

    /**
     * The socket failure is already being recovered by [RtcSession.stateJob].
     */
    data class RecoverableSocketFailure(
        override val error: Exception,
    ) : SfuConnectFailure

    /**
     * The socket failure should fail immediately without recovery.
     */
    data class TerminalSocketFailure(
        override val error: Exception,
    ) : SfuConnectFailure
}
