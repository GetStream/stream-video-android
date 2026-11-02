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

package io.getstream.video.android.core.call.components

import io.getstream.android.video.api.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.events.GoAwayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Owns the event pipeline for a call: the shared [events] flow and the dispatch / handling of
 * incoming [VideoEvent]s.
 */
internal class CallEventManager(
    private val type: String,
    private val id: String,
    private val scope: CoroutineScope,
    // Lazy provider: the reconnector is built after the event pipeline, because `CallState` reads
    // `events` while constructing and the reconnector transitively needs the state.
    private val reconnector: () -> CallReconnector,
) {
    private val logger by taggedLogger("Call:EventManager:$type:$id")

    val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)

    fun handleEvent(event: VideoEvent) {
        logger.v { "[call handleEvent] #sfu; event.type: ${event.getEventType()}" }

        when (event) {
            is GoAwayEvent ->
                scope.launch {
                    reconnector().migrate()
                }
        }
    }

    fun fireEvent(event: VideoEvent) {
        if (!events.tryEmit(event)) {
            logger.e { "Failed to emit event to observers: [event: $event]" }
        }
    }
}
