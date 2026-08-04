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

package io.getstream.video.android.core.analytics.coordinator

import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

internal class CoordinatorAnalytics(
    private val observerScope: CoroutineScope,
    private val eventReporter: ClientEventReporter,
    private val stateHolder: CoordinatorAnalyticsStateHolder,
) {

    private var job: Job? = null
    private var stageId = MutableStateFlow("")

    /**
     * Observes core [StreamConnectionState] for coordinator WS connect analytics.
     *
     * Reports initiated on the first transition into [StreamConnectionState.Connecting]
     * while no stage is in flight (reconnects while a stage is already open are ignored).
     */
    internal fun startObserver(connectionState: StateFlow<StreamConnectionState>) {
        endObserver()
        job = observerScope.launch {
            connectionState.collect {
                // because demo-app invokes coordinator join before user id is ready
                val userIdIsNotNull = StreamVideo.instanceOrNull()?.userId != null
                if (userIdIsNotNull) {
                    when (it) {
                        is StreamConnectionState.Connecting -> {
                            if (stageId.value.isEmpty()) {
                                stateHolder.updateCoordinatorConnectId(UUID.randomUUID().toString())
                                stageId.value = eventReporter.reportCoordinatorWSInitiated()
                            }
                        }

                        is StreamConnectionState.Connected -> {
                            if (stageId.value.isNotEmpty()) {
                                eventReporter.reportCoordinatorWSCompleted(
                                    stageId.value,
                                    true,
                                    0,
                                )
                                stageId.value = ""
                            }
                        }

                        is StreamConnectionState.Disconnected -> {
                            if (stageId.value.isNotEmpty()) {
                                eventReporter.reportCoordinatorWSCompleted(
                                    stageId.value,
                                    false,
                                    0,
                                )
                                stageId.value = ""
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    fun endObserver() {
        job?.cancel()
        job = null
        stageId.value = ""
    }
}
