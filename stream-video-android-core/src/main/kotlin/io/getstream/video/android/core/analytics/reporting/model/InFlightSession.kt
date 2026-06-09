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

package io.getstream.video.android.core.analytics.reporting.model

import io.getstream.video.android.core.analytics.call.observer.model.JoinReason

internal typealias StageId = String
internal typealias CallId = String

internal sealed class InFlightSession(
    open val stage: EventStage.Call,
    open val startedAtMs: Long,
    open val stageId: StageId,
)

internal data class PostCallFlightSession(
    val callId: String,
    val callType: String,
    override val stageId: StageId,
    override val stage: EventStage.Call,
    override val startedAtMs: Long,
    val joinStageAttemptIdSnapshot: String,
    val joinReason: JoinReason,
    val sfuId: String? = null,
    val callSessionId: String? = null,
    val userSessionId: String? = null,
    val peerConnectionRole: PeerConnectionRole? = null,
    val wasPreviouslyConnected: Boolean = false,

) : InFlightSession(stage, startedAtMs, stageId)

internal data class PreCallInFlightSession(
    override val stage: EventStage.Call,
    override val startedAtMs: Long,
    override val stageId: StageId,
    val coordinatorConnectId: String,
) : InFlightSession(stage, startedAtMs, stageId)
