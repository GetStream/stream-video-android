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

import io.getstream.video.android.core.analytics.call.observer.model.Stage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SfuAnalyticsStateHolder {
    private val _sfuId = MutableStateFlow("")
    val sfuId: StateFlow<String> = _sfuId.asStateFlow()

    private val _stage = MutableStateFlow(Stage.NOT_STARTED)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _stageId = MutableStateFlow("")
    val stageId: StateFlow<String> = _stageId.asStateFlow()

    fun updateSfuId(sfuId: String) {
        _sfuId.value = sfuId
    }

    fun updateStage(stage: Stage) {
        _stage.value = stage
    }

    fun updateStageId(eventStageId: String) {
        _stageId.value = eventStageId
    }
}
