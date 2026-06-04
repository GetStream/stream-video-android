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

package io.getstream.video.android.core.analytics.reporting

import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.android.video.generated.models.ReportClientEventRequest
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Responsible for delivering telemetry [ClientEvent]s to the backend.
 * Failed events are handed to a [PendingEventStore] and can be retried via [retryPending].
 */
internal interface EventSender {
    fun send(event: ClientEvent)
    fun sendAll(events: List<ClientEvent>)

    /**
     * Retries any events that previously failed to send.
     * Call this when connectivity is restored or on a suitable recovery point.
     */
    fun retryPending()
}

/**
 * Sends each event immediately in a coroutine.
 * On network failure the events are saved to [pendingStore] for later retry.
 */
internal class ImmediateEventSender(
    private val api: ProductvideoApi,
    private val scope: CoroutineScope = UserScope(ClientScope()),
    private val pendingStore: PendingEventStore = InMemoryPendingEventStore(),
) : EventSender {

    private val logger by taggedLogger("ImmediateEventSender")

    override fun send(event: ClientEvent) = sendAll(listOf(event))

    override fun sendAll(events: List<ClientEvent>) {
        if (events.isEmpty()) return
        scope.launch {
            runCatching {
                logger.d { events.joinToString(",") { it.toLog() } }
                api.reportClientCallEvent(ReportClientEventRequest(events))
            }.onFailure { e ->
                logger.w { "[sendAll] Failed — saving ${events.size} event(s) for retry: ${e.message}" }
                pendingStore.save(events)
            }
        }
    }

    override fun retryPending() {
        if (pendingStore.isEmpty()) return
        val pending = pendingStore.loadAndClear()
        sendAll(pending)
    }
}
