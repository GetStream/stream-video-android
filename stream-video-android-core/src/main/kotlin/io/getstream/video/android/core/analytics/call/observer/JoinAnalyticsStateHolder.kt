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

package io.getstream.video.android.core.analytics.call.observer

import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class JoinAnalyticsStateHolder {

    private val _state = MutableStateFlow(JoinTelemetryState())
    val state: StateFlow<JoinTelemetryState> = _state.asStateFlow()

    fun updateJoinReason(reason: JoinReason?) {
        _state.update { current ->
            current.copy(joinReason = reason)
        }
    }

    fun updateJoinStageAttemptId(attemptId: String?) {
        _state.update { current ->
            current.copy(joinStageAttemptId = attemptId)
        }
    }

    fun update(
        reason: JoinReason?,
        attemptId: String?,
    ) {
        _state.update {
            it.copy(
                joinReason = reason,
                joinStageAttemptId = attemptId,
            )
        }
    }

    fun reset() {
        _state.value = JoinTelemetryState()
    }
}

internal data class JoinTelemetryState(
    val joinReason: JoinReason? = null,
    val joinStageAttemptId: String? = null,
)
