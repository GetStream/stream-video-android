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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class PeerConnectionAnalyticsStateHolder {
    private val _state = MutableStateFlow(PeerConnectionAnalyticsState())
    val state: StateFlow<PeerConnectionAnalyticsState> = _state.asStateFlow()

    fun updatePeerConnectionObserverJob(job: Job?) {
        _state.update { it.copy(peerConnectionObserverJob = job) }
    }

    fun updatePublisherJob(job: Job?) {
        _state.update { it.copy(publisherJob = job) }
    }

    fun updateSubscriberJob(job: Job?) {
        _state.update { it.copy(subscriberJob = job) }
    }

    fun updatePublisherStage(stage: Stage) {
        _state.update { it.copy(publisherStage = stage) }
    }

    fun updateSubscriberStage(stage: Stage) {
        _state.update { it.copy(subscriberStage = stage) }
    }

    fun update(
        peerConnectionObserverJob: Job? = state.value.peerConnectionObserverJob,
        publisherJob: Job? = state.value.publisherJob,
        subscriberJob: Job? = state.value.subscriberJob,
        publisherStage: Stage = state.value.publisherStage,
        subscriberStage: Stage = state.value.subscriberStage,
    ) {
        _state.update {
            it.copy(
                peerConnectionObserverJob = peerConnectionObserverJob,
                publisherJob = publisherJob,
                subscriberJob = subscriberJob,
                publisherStage = publisherStage,
                subscriberStage = subscriberStage,
            )
        }
    }

    fun reset() {
        _state.value = PeerConnectionAnalyticsState()
    }
}

internal data class PeerConnectionAnalyticsState(
    var peerConnectionObserverJob: Job? = null,
    var publisherJob: Job? = null,
    var subscriberJob: Job? = null,
    val publisherStage: Stage = Stage.NOT_STARTED,
    val subscriberStage: Stage = Stage.NOT_STARTED,
)
