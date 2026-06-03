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

package io.getstream.video.android.core.analytics

import io.getstream.video.android.core.events.reporting.ClientEventReporter
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketStateService
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketConnectionType
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class CoordinatorAnalytics(
    private val scope: CoroutineScope,
    private val eventReporter: ClientEventReporter,
) {

    private var job: Job? = null
    private var stageId = ""

    fun startObserver(videoSocketStateFlow: StateFlow<VideoSocketState>) {
        endObserver()
        job = scope.launch {
            videoSocketStateFlow.collect {
                when (it) {
                    is VideoSocketState.Connecting -> {
                        when (it.connectionType) {
                            VideoSocketConnectionType.INITIAL_CONNECTION -> {
                                stageId = eventReporter.reportCoordinatorWSInitiated()
                            }

                            VideoSocketConnectionType.AUTOMATIC_RECONNECTION -> {}
                            VideoSocketConnectionType.FORCE_RECONNECTION -> {}
                        }
                    }

                    is VideoSocketState.Connected -> {
                        if (stageId.isNotEmpty()) {
                            eventReporter.reportCoordinatorWSCompleted(
                                stageId,
                                true,
                                CoordinatorSocketStateService.lastRetryAttempts,
                            )
                        }
                    }

                    is VideoSocketState.Disconnected.DisconnectedPermanently -> {
                        if (stageId.isNotEmpty()) {
                            eventReporter.reportCoordinatorWSCompleted(
                                stageId,
                                false,
                                CoordinatorSocketStateService.lastRetryAttempts,
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun endObserver() {
        job?.cancel()
        job = null
    }
}
