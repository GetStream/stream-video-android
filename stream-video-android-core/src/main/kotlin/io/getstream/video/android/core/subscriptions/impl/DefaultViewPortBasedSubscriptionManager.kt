/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.subscriptions.impl

import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.subscriptions.api.DebouncePolicy
import io.getstream.video.android.core.subscriptions.api.DebouncedSubscriptionManager
import io.getstream.video.android.core.subscriptions.api.ViewportBasedSubscriptionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails
import java.util.Collections

/**
 * A SubscriptionManager that manages subscriptions based on viewport logic, using DefaultSubscriptionManager for core operations.
 */
internal class DefaultViewPortBasedSubscriptionManager(
    private val defaultManager: DebouncedSubscriptionManager,
) :
    ViewportBasedSubscriptionManager {

    private val logger by taggedLogger("SubscriptionManager#ViewPort")

    // Map of (sessionId, trackType, dimension) to set of viewportIds that are currently visible
    private val viewportSubscriptions =
        Collections.synchronizedMap(
            mutableMapOf<Triple<String, TrackType, VideoDimension?>, MutableSet<String>>(),
        )

    private val mutex = Mutex()

    @JvmSynthetic
    override suspend fun updateViewport(
        viewportId: String,
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimension: VideoDimension,
    ) {
        logger.v {
            "[updateViewport] viewportId=$viewportId, sessionId=$sessionId, trackType=$trackType, visible=$visible, dimension=$dimension"
        }
        val key = Triple(sessionId, trackType, dimension)
        mutex.withLock {
            val ids = viewportSubscriptions.getOrPut(key) { mutableSetOf() }
            if (visible) {
                ids.add(viewportId)
                logger.d { "Added viewportId=$viewportId for key=$key. Current ids: $ids" }
                // Subscribe immediately if this is the first visible viewport for this track
                if (ids.size == 1) {
                    logger.i { "First visible viewport for $key, subscribing immediately." }
                    subscribe(listOf(key))
                }
            } else {
                ids.remove(viewportId)
                logger.d { "Removed viewportId=$viewportId for key=$key. Remaining ids: $ids" }
                if (ids.isEmpty()) {
                    viewportSubscriptions.remove(key)
                    logger.i { "No more visible viewports for $key, scheduling debounced unsubscribe." }
                    unsubscribeDebounced(sessionId, trackType)
                }
            }
        }
    }

    override fun setDebouncePolicy(policy: DebouncePolicy) =
        defaultManager.setDebouncePolicy(policy)

    override fun unsubscribeDebounced(sessionId: String, trackType: TrackType) =
        defaultManager.unsubscribeDebounced(sessionId, trackType)

    override fun subscriptions(): Map<String, TrackSubscriptionDetails> =
        defaultManager.subscriptions()

    override suspend fun subscribe(sessions: List<Triple<String, TrackType, VideoDimension>>): Result<List<TrackSubscriptionDetails>> =
        defaultManager.subscribe(sessions)

    override suspend fun subscribe(
        sessionId: String,
        trackType: TrackType,
        dimension: VideoDimension,
    ): Result<TrackSubscriptionDetails> =
        defaultManager.subscribe(sessionId, trackType, dimension)

    override suspend fun unsubscribe(sessions: List<Pair<String, TrackType>>): Result<Unit> =
        defaultManager.unsubscribe(sessions)

    override suspend fun unsubscribe(sessionId: String, trackType: TrackType): Result<Unit> =
        defaultManager.unsubscribe(sessionId, trackType)

    override fun clear() = synchronized(viewportSubscriptions) {
        viewportSubscriptions.clear()
        defaultManager.clear()
    }
}
