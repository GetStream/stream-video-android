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

import io.getstream.result.Result
import io.getstream.video.android.core.subscriptions.api.DebouncePolicy
import io.getstream.video.android.core.subscriptions.api.ManualOverridesSubscriptionManager
import io.getstream.video.android.core.subscriptions.api.ViewportBasedSubscriptionManager
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails
import java.util.concurrent.ConcurrentHashMap

/**
 * A SubscriptionManager that allows manual overrides for visibility (true/false) and video dimensions per sessionId.
 */
internal class DefaultManualOverridesSubscriptionManager(
    private val delegate: ViewportBasedSubscriptionManager,
) : ManualOverridesSubscriptionManager {

    private val visibilityOverrides = ConcurrentHashMap<String, Boolean>()
    private val dimensionOverrides = ConcurrentHashMap<String, VideoDimension>()
    private var globalVisibilityOverride: Boolean? = null
    private var globalDimensionOverride: VideoDimension? = null

    override fun setVisibilityOverride(sessionId: String, visible: Boolean) {
        visibilityOverrides[sessionId] = visible
    }

    override fun removeVisibilityOverride(sessionId: String) {
        visibilityOverrides.remove(sessionId)
    }

    override fun setDimensionOverride(sessionId: String, dimension: VideoDimension) {
        dimensionOverrides[sessionId] = dimension
    }

    override fun removeDimensionOverride(sessionId: String) {
        dimensionOverrides.remove(sessionId)
    }

    override fun clearOverrides(sessionId: String) {
        visibilityOverrides.remove(sessionId)
        dimensionOverrides.remove(sessionId)
    }

    override fun setGlobalVisibilityOverride(visible: Boolean?) {
        globalVisibilityOverride = visible
    }

    override fun setGlobalDimensionOverride(dimension: VideoDimension?) {
        globalDimensionOverride = dimension
    }

    override fun clearGlobalOverrides() {
        globalVisibilityOverride = null
        globalDimensionOverride = null
    }

    override suspend fun updateViewport(
        viewportId: String,
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimension: VideoDimension,
    ) {
        val effectiveVisible = globalVisibilityOverride ?: visibilityOverrides[sessionId] ?: visible
        val effectiveDimension = globalDimensionOverride ?: dimensionOverrides[sessionId] ?: dimension
        delegate.updateViewport(
            viewportId,
            sessionId,
            trackType,
            effectiveVisible,
            effectiveDimension,
        )
    }

    override fun setDebouncePolicy(policy: DebouncePolicy) = delegate.setDebouncePolicy(policy)
    override fun unsubscribeDebounced(
        sessionId: String,
        trackType: TrackType,
    ) = delegate.unsubscribeDebounced(sessionId, trackType)
    override fun subscriptions(): Map<String, TrackSubscriptionDetails> = delegate.subscriptions()
    override suspend fun subscribe(
        sessions: List<Triple<String, TrackType, VideoDimension>>,
    ): Result<List<TrackSubscriptionDetails>> =
        delegate.subscribe(sessions)
    override suspend fun unsubscribe(
        sessions: List<Pair<String, TrackType>>,
    ): Result<Unit> = delegate.unsubscribe(sessions)
    override suspend fun unsubscribe(
        sessionId: String,
        trackType: TrackType,
    ): Result<Unit> = delegate.unsubscribe(sessionId, trackType)
    override suspend fun subscribe(
        sessionId: String,
        trackType: TrackType,
        dimension: VideoDimension,
    ): Result<TrackSubscriptionDetails> = delegate.subscribe(sessionId, trackType, dimension)
    override fun clear() = delegate.clear()
}
