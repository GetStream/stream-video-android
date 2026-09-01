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

package io.getstream.video.android.core.analytics.call.observer.model

internal data class JoinAnalyticsModel(val retryAttempt: Int, val joinReason: JoinReason? = null)
internal sealed class JoinReason {

    abstract val message: String

    data object FirstAttempt : JoinReason() {
        override val message: String = "first-attempt"
    }

    data object ReJoin : JoinReason() {
        override val message: String = "full-rejoin"
    }

    data object Migrate : JoinReason() {
        override val message: String = "migrate"
    }

    data object Unknown : JoinReason() {
        override val message: String = "unknown"
    }

    data class Custom(
        override val message: String,
    ) : JoinReason()

    /**
     * Analytics-only reason for a `Call.join()` invocation made while another join attempt is active.
     *
     * [originalStageId] identifies the active attempt this invocation runs concurrently with.
     * This should not be used as a Coordinator or SFU rejoin reason.
     */
    data class ConcurrentWith(
        val originalStageId: String,
        override val message: String = "concurrent-with:$originalStageId",
    ) : JoinReason()
}

/**
 * Describes the analytics metadata for one invocation of the public `Call.join()` API.
 *
 * [Standalone] represents an invocation made without another join in flight. [Concurrent] represents
 * an invocation made while another join is active and links the new invocation to that active
 * attempt. A concurrent attempt does not start a separate join execution because concurrent calls
 * are coalesced by the join coordinator.
 *
 * @property stageId The unique stage-attempt ID assigned to this API invocation.
 */
internal sealed interface JoinInvocation {
    val stageId: String

    data class Standalone(
        override val stageId: String,
    ) : JoinInvocation

    data class Concurrent(
        val activeStageAttemptId: String,
        override val stageId: String,
    ) : JoinInvocation
}
