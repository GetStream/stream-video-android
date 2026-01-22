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

package io.getstream.video.android.core.call

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.events.GoAwayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class CallEventManager(
    private val events: MutableSharedFlow<VideoEvent>,
    private val sessionManager: CallSessionManager,
    private val callScope: CoroutineScope, // TODO Rahul must check if this still working after leave() join()
    private val subscriptionsProvider: () -> Set<EventSubscription>,
) {

    private val logger by taggedLogger("CallEventManager")

    fun fireEvent(event: VideoEvent) = synchronized(subscriptionsProvider.invoke()) {
        subscriptionsProvider.invoke().forEach { sub ->
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

    fun handleEvent(event: VideoEvent) {
        logger.v { "[call handleEvent] #sfu; event.type: ${event.getEventType()}" }

        when (event) {
            is GoAwayEvent ->
                callScope.launch {
                    sessionManager.migrate()
                }
        }
    }
}
