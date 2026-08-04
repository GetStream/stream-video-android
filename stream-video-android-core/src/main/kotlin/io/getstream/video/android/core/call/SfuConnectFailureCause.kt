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
 * Typed cause of a failed SFU connect attempt. This reports what happened at
 * the socket/session boundary; callers decide how to recover from each cause.
 */
internal sealed interface SfuConnectFailureCause {
    /**
     * [RtcSession.connectInternal] stopped waiting for the socket to reach a terminal state.
     */
    data object SocketStateObservationTimeout : SfuConnectFailureCause

    /**
     * The socket failure is already being recovered by [RtcSession.stateJob].
     */
    data object RecoverableSocketFailure : SfuConnectFailureCause

    /**
     * The socket failure should fail immediately without recovery.
     */
    data object TerminalSocketFailure : SfuConnectFailureCause
}
