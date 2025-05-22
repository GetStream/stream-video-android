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

package io.getstream.video.android.core.subscriptions.api

import io.getstream.result.Result
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails

/**
 * Manages subscriptions to tracks.
 */
internal interface SubscriptionManager {

    /**
     * Get the current subscriptions.
     *
     * @return A map of session IDs to track subscription details.
     */
    fun subscriptions(): Map<String, TrackSubscriptionDetails>

    /**
     * Subscribe to a list of tracks.
     *
     * @param sessions The list of sessions to subscribe to.
     * @param dimension The video dimension to use for all subscriptions.
     */
    suspend fun subscribe(
        sessions: List<Triple<String, TrackType, VideoDimension>>,
    ): Result<List<TrackSubscriptionDetails>>

    /**
     * Unsubscribe from a list of tracks.
     *
     * @param sessions The list of sessions to unsubscribe from.
     */
    suspend fun unsubscribe(sessions: List<Pair<String, TrackType>>): Result<Unit>

    /**
     * Subscribe to a track.
     *
     * @param sessionId The session ID of the participant.
     * @param trackType The track type to subscribe to.
     * @param dimension The video dimension to use for the subscription.
     */
    suspend fun subscribe(
        sessionId: String,
        trackType: TrackType,
        dimension: VideoDimension,
    ): Result<TrackSubscriptionDetails>

    /**
     * Unsubscribe from a track.
     *
     * @param sessionId The session ID of the participant.
     * @param trackType The track type to unsubscribe from.
     */
    suspend fun unsubscribe(
        sessionId: String,
        trackType: TrackType,
    ): Result<Unit>

    /**
     * Clear all subscriptions.
     */
    fun clear()
}

internal interface DebouncedSubscriptionManager : SubscriptionManager {

    /**
     * Set the debounce policy for the subscription manager.
     *
     * @param policy The debounce policy to use.
     */
    fun setDebouncePolicy(policy: DebouncePolicy)

    /**
     * Unsubscribe from a track with a debounce policy.
     *
     * @param sessionId The session ID of the participant.
     * @param trackType The track type to unsubscribe from.
     */
    fun unsubscribeDebounced(
        sessionId: String,
        trackType: TrackType,
    )
}

/**
 * A subscription manager that is based on the viewport.
 */
internal interface ViewportBasedSubscriptionManager : DebouncedSubscriptionManager {

    /**
     * Update the viewport.
     *
     * @param viewportId The viewport ID.
     * @param sessionId The session ID of the participant.
     * @param trackType The track type.
     * @param visible Whether the track is visible or not.
     */
    suspend fun updateViewport(
        viewportId: String,
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimension: VideoDimension,
    )
}

/**
 * A viewport based subscription manager that allows manual overrides for visibility and video dimensions.
 */
internal interface ManualOverridesSubscriptionManager : ViewportBasedSubscriptionManager {
    /**
     * Set a visibility override for a session ID.
     */
    fun setVisibilityOverride(sessionId: String, visible: Boolean)

    /**
     * Remove a visibility override for a session ID.
     */
    fun removeVisibilityOverride(sessionId: String)

    /**
     * Set a dimension override for a session ID.
     */
    fun setDimensionOverride(sessionId: String, dimension: VideoDimension)

    /**
     * Remove a dimension override for a session ID.
     */
    fun removeDimensionOverride(sessionId: String)

    /**
     * Clear all overrides for a session ID.
     */
    fun clearOverrides(sessionId: String)

    /**
     * Set a global visibility override for all session IDs.
     * If set, this takes precedence over per-session overrides.
     */
    fun setGlobalVisibilityOverride(visible: Boolean?)

    /**
     * Set a global dimension override for all session IDs.
     * If set, this takes precedence over per-session overrides.
     */
    fun setGlobalDimensionOverride(dimension: VideoDimension?)

    /**
     * Clear all global overrides.
     */
    fun clearGlobalOverrides()
}

/**
 * A debounce policy for the subscription manager.
 */
internal interface DebouncePolicy {
    /**
     * The debounce time in milliseconds.
     */
    fun debounceTime(): Long
}
