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
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.subscriptions.api.SubscriptionManager
import io.getstream.video.android.core.utils.safeCallWithResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import java.util.Collections

internal class DefaultSubscriptionManager(
    private val sessionId: String,
    private val signalingService: SignalServerService,
) : SubscriptionManager {

    private val logger by taggedLogger("SubscriptionManager")
    private val subscriptions: MutableMap<String, TrackSubscriptionDetails> =
        Collections.synchronizedMap(mutableMapOf())
    private val mutex = Mutex()

    override fun subscriptions(): Map<String, TrackSubscriptionDetails> = subscriptions

    override suspend fun subscribe(sessions: List<Triple<String, TrackType, VideoDimension>>): Result<List<TrackSubscriptionDetails>> =
        mutex.withLock {
            logger.v { "[subscribe] Subscribing to sessions: $sessions" }
            safeCallWithResult {
                val newSubscriptions = sessions.map { (sessionId, trackType, dimension) ->
                    TrackSubscriptionDetails(
                        session_id = sessionId,
                        track_type = trackType,
                        dimension = dimension,
                    )
                }
                subscriptions.putAll(newSubscriptions.associateBy { it.session_id })
                logger.d { "[subscribe] Subscriptions after add: ${subscriptions.keys}" }
                syncSubscriptions(signalingService, subscriptions)
                newSubscriptions
            }
        }

    override suspend fun subscribe(
        sessionId: String,
        trackType: TrackType,
        dimension: VideoDimension,
    ): Result<TrackSubscriptionDetails> = mutex.withLock {
        logger.v {
            "[subscribe] Subscribing to sessionId=$sessionId, trackType=$trackType, dimension=$dimension"
        }
        safeCallWithResult {
            val subscription = TrackSubscriptionDetails(
                session_id = sessionId,
                track_type = trackType,
                dimension = dimension,
            )
            subscriptions[sessionId] = subscription
            logger.d { "[subscribe] Subscriptions after add: ${subscriptions.keys}" }
            syncSubscriptions(signalingService, subscriptions)
            subscription
        }
    }

    override suspend fun unsubscribe(sessions: List<Pair<String, TrackType>>): Result<Unit> =
        mutex.withLock {
            logger.v { "[unsubscribe] Unsubscribing from sessions: $sessions" }
            safeCallWithResult {
                removeSubscriptions(sessions)
                logger.d { "[unsubscribe] Subscriptions after remove: ${subscriptions.keys}" }
                syncSubscriptions(signalingService, subscriptions).getOrThrow()
            }
        }

    override suspend fun unsubscribe(
        sessionId: String,
        trackType: TrackType,
    ): Result<Unit> = mutex.withLock {
        logger.v { "[unsubscribe] Unsubscribing from sessionId=$sessionId, trackType=$trackType" }
        safeCallWithResult {
            subscriptions.remove(sessionId)
            logger.d { "[unsubscribe] Subscriptions after remove: ${subscriptions.keys}" }
            syncSubscriptions(signalingService, subscriptions).getOrThrow()
        }
    }

    override fun clear() = synchronized(subscriptions) {
        subscriptions.clear()
    }

    /**
     * Removes all subscriptions matching the given sessionId and trackType pairs.
     */
    private fun removeSubscriptions(sessions: List<Pair<String, TrackType>>) {
        sessions.forEach { (sessionId, trackType) ->
            val sub = subscriptions[sessionId]
            if (sub != null && sub.track_type == trackType) {
                subscriptions.remove(sessionId)
            }
        }
    }

    private suspend fun syncSubscriptions(
        signalingService: SignalServerService,
        subscriptions: Map<String, TrackSubscriptionDetails>,
    ): Result<Unit> {
        val copy = subscriptions.values.toList()
        logger.v { "[syncSubscriptionsImmediate] #subscription; Current subscriptions: $copy" }
        val request = UpdateSubscriptionsRequest(
            session_id = sessionId,
            tracks = copy,
        )
        logger.v { "[syncSubscriptionsImmediate] #subscription; Sending request: $request" }
        val response = signalingService.updateSubscriptions(request)
        val result = if (response.error != null) {
            logger.e { response.error.message }
            Result.Failure(
                Error.ThrowableError(
                    "Failed to update subscriptions",
                    Throwable(response.error.message),
                ),
            )
        } else {
            Result.Success(Unit)
        }
        return result
    }
}
