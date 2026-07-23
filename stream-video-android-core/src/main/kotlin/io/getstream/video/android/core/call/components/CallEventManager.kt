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

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.VideoEventListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Owns the event pipeline for a [Call]: the shared [events] flow, the set of legacy
 * [EventSubscription]s, and the dispatch / handling of incoming [VideoEvent]s.
 */
internal class CallEventManager(
    private val call: Call,
) {
    private val logger by taggedLogger("Call:EventManager:${call.type}:${call.id}")

    val events = MutableSharedFlow<VideoEvent>(extraBufferCapacity = 150)

    private val subscriptions = Collections.synchronizedSet(mutableSetOf<EventSubscription>())

    fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = synchronized(subscriptions) {
        val filter = { event: VideoEvent ->
            eventTypes.any { type -> type.isInstance(event) }
        }
        val sub = EventSubscription(listener, filter)
        subscriptions.add(sub)
        return sub
    }

    fun subscribe(
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription = synchronized(subscriptions) {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    fun unsubscribe(eventSubscription: EventSubscription) = synchronized(subscriptions) {
        subscriptions.remove(eventSubscription)
    }

    fun handleEvent(event: VideoEvent) {
        logger.v { "[call handleEvent] #sfu; event.type: ${event.getEventType()}" }

        when (event) {
            is GoAwayEvent ->
                call.scope.launch {
                    call.migrate()
                }
        }
    }

    fun fireEvent(event: VideoEvent) = synchronized(subscriptions) {
        subscriptions.forEach { sub ->
            if (!sub.isDisposed) {
                // subs without filters should always fire
                if (sub.filter == null) {
                    sub.listener.onEvent(event)
                }

                // if there is a filter, check it and fire if it matches
                sub.filter?.let {
                    if (it.invoke(event)) {
                        sub.listener.onEvent(event)
                    }
                }
            }
        }

        if (!events.tryEmit(event)) {
            logger.e { "Failed to emit event to observers: [event: $event]" }
        }
    }
}
