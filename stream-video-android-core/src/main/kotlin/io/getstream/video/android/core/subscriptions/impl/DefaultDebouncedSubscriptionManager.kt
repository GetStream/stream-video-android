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

import androidx.annotation.VisibleForTesting
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.subscriptions.api.DebouncePolicy
import io.getstream.video.android.core.subscriptions.api.DebouncedSubscriptionManager
import io.getstream.video.android.core.subscriptions.api.SubscriptionManager
import io.getstream.video.android.core.utils.safeCallWithResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails
import java.util.Collections

internal class DefaultDebouncedSubscriptionManager(
    private val scope: CoroutineScope,
    private val delegate: SubscriptionManager,
    private var debouncePolicy: DebouncePolicy = DebouncePolicies.default(2000),
) : DebouncedSubscriptionManager {
    private val logger by taggedLogger("SubscriptionManager#Debounced")
    private data class PendingUnsubscribe(
        val sessionId: String,
        val trackType: TrackType,
        val timestamp: Long,
    )
    private val pendingUnsubscribes =
        Collections.synchronizedList(mutableListOf<PendingUnsubscribe>())
    private var internalPulseJob: Job? = null
    private fun startPulseIfNotStarted() {
        if (internalPulseJob == null) {
            internalPulseJob = scope.launch {
                while (true) {
                    delay(debouncePolicy.debounceTime())
                    processPendingUnsubscribes()
                }
            }
        }
    }

    // API
    override fun setDebouncePolicy(policy: DebouncePolicy) {
        debouncePolicy = policy
    }

    override fun subscriptions(): Map<String, TrackSubscriptionDetails> = delegate.subscriptions()

    override suspend fun subscribe(sessions: List<Triple<String, TrackType, VideoDimension>>): Result<List<TrackSubscriptionDetails>> =
        delegate.subscribe(sessions)

    override suspend fun subscribe(
        sessionId: String,
        trackType: TrackType,
        dimension: VideoDimension,
    ): Result<TrackSubscriptionDetails> {
        logger.v {
            "[subscribe] Subscribing to sessionId=$sessionId, trackType=$trackType, dimension=$dimension"
        }
        // Remove any pending unsubscribe for this track
        synchronized(pendingUnsubscribes) {
            pendingUnsubscribes.removeAll { it.sessionId == sessionId && it.trackType == trackType }
        }
        // Only call delegate if not already subscribed (i.e., not in delegate.subscriptions())
        val alreadySubscribed = delegate.subscriptions()[sessionId]?.track_type == trackType
        if (alreadySubscribed) {
            logger.d { "[subscribe] Already subscribed to sessionId=$sessionId, trackType=$trackType" }
            return Result.Success(delegate.subscriptions()[sessionId]!!)
        } else {
            logger.d {
                "[subscribe] Not yet subscribed, delegating subscribe for sessionId=$sessionId, trackType=$trackType, dimension=$dimension"
            }
            return delegate.subscribe(sessionId, trackType, dimension)
        }
    }

    override suspend fun unsubscribe(sessions: List<Pair<String, TrackType>>): Result<Unit> {
        logger.v { "[unsubscribe] Unsubscribing from sessions: $sessions" }
        return delegate.unsubscribe(sessions)
    }

    override suspend fun unsubscribe(
        sessionId: String,
        trackType: TrackType,
    ): Result<Unit> {
        logger.v { "[unsubscribe] Unsubscribing from sessionId=$sessionId, trackType=$trackType" }
        return delegate.unsubscribe(sessionId, trackType)
    }

    override fun unsubscribeDebounced(
        sessionId: String,
        trackType: TrackType,
    ) {
        logger.v {
            "[unsubscribeDebounced] Scheduling debounced unsubscribe for sessionId=$sessionId, trackType=$trackType"
        }
        synchronized(pendingUnsubscribes) {
            pendingUnsubscribes.add(
                PendingUnsubscribe(
                    sessionId,
                    trackType,
                    System.currentTimeMillis(),
                ),
            )
        }
        startPulseIfNotStarted()
    }

    override fun clear() = delegate.clear()

    @VisibleForTesting
    internal fun pulsingJob(): Job? = internalPulseJob

    @VisibleForTesting
    internal fun pendingUnsubscribes(): List<Triple<String, TrackType, Long>> = pendingUnsubscribes.map {
        Triple(it.sessionId, it.trackType, it.timestamp)
    }

    @VisibleForTesting
    internal suspend fun processPendingUnsubscribes(now: Long = System.currentTimeMillis()) {
        val toUnsubscribe = mutableListOf<Pair<String, TrackType>>()
        synchronized(pendingUnsubscribes) {
            val iterator = pendingUnsubscribes.iterator()
            while (iterator.hasNext()) {
                val pending = iterator.next()
                if (now - pending.timestamp >= 2000) {
                    toUnsubscribe.add(pending.sessionId to pending.trackType)
                    iterator.remove()
                }
            }
        }
        if (toUnsubscribe.isNotEmpty()) {
            safeCallWithResult {
                delegate.unsubscribe(toUnsubscribe)
            }
        }
    }
}
