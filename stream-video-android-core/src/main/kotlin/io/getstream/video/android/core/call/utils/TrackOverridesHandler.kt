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

package io.getstream.video.android.core.call.utils

import io.getstream.log.TaggedLogger
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails

/**
 * Handles incoming video track overrides (resolution and visibility).
 *
 * @param onOverridesUpdate Lambda used to notify the caller when the overrides are updated.
 * @param logger Logger to be used.
 */
internal class TrackOverridesHandler(
    private val onOverridesUpdate: (overrides: Map<String, TrackOverride>) -> Unit,
    private val logger: TaggedLogger? = null,
) {

    private val trackOverrides: MutableMap<String, TrackOverride> = mutableMapOf()

    data class TrackOverride(
        val dimensions: VideoDimension? = null,
        val visible: Boolean? = null,
    )

    /**
     * Updates incoming video dimensions overrides.
     *
     * @param sessionIds List of session IDs to update. If `null`, the override will be applied to all participants.
     * @param dimensions Video dimensions to set. Set to `null` to switch back to auto.
     */
    fun updateOverrides(
        sessionIds: List<String>? = null,
        dimensions: VideoDimension? = null,
    ) {
        val putDimensions = dimensions

        if (sessionIds == null) {
            putOrRemoveDimensionsOverride(
                sessionId = ALL_PARTICIPANTS,
                putDimensions = putDimensions,
                existingVisibility = trackOverrides[ALL_PARTICIPANTS]?.visible,
            )
        } else {
            sessionIds.forEach { sessionId ->
                putOrRemoveDimensionsOverride(
                    sessionId = sessionId,
                    putDimensions = putDimensions,
                    existingVisibility = trackOverrides[sessionId]?.visible,
                )
            }
        }

        onOverridesUpdate(trackOverrides)

        logger?.d { "[updateOverrides] #manual-quality-selection; Overrides: $trackOverrides" }
    }

    private fun putOrRemoveDimensionsOverride(sessionId: String, putDimensions: VideoDimension?, existingVisibility: Boolean?) {
        if (putDimensions == null && (existingVisibility == true || existingVisibility == null)) {
            trackOverrides.remove(sessionId)
        } else {
            trackOverrides.put(
                sessionId,
                TrackOverride(dimensions = putDimensions, visible = existingVisibility),
            )
        }
    }

    /**
     * Updates incoming video visibility overrides.
     *
     * @param sessionIds List of session IDs to update. If `null`, the override will be applied to all participants.
     * @param visible Video visibility to set. Set to `null` to switch back to auto.
     */
    fun updateOverrides(
        sessionIds: List<String>? = null,
        visible: Boolean? = null,
    ) {
        val putVisibility = visible

        if (sessionIds == null) {
            putOrRemoveVisibilityOverride(
                sessionId = ALL_PARTICIPANTS,
                putVisibility = putVisibility,
                existingDimensions = trackOverrides[ALL_PARTICIPANTS]?.dimensions,
            )
        } else {
            sessionIds.forEach { sessionId ->
                putOrRemoveVisibilityOverride(
                    sessionId = sessionId,
                    putVisibility = putVisibility,
                    existingDimensions = trackOverrides[sessionId]?.dimensions,
                )
            }
        }

        onOverridesUpdate(trackOverrides)

        logger?.d { "[updateOverrides] #manual-quality-selection; Overrides: $trackOverrides" }
    }

    private fun putOrRemoveVisibilityOverride(sessionId: String, putVisibility: Boolean?, existingDimensions: VideoDimension?) {
        if (existingDimensions == null && (putVisibility == true || putVisibility == null)) {
            trackOverrides.remove(sessionId)
        } else {
            trackOverrides.put(
                sessionId,
                TrackOverride(dimensions = existingDimensions, visible = putVisibility),
            )
        }
    }

    /**
     * Applies overrides to given list of tracks.
     *
     * @return List of tracks with visibility and dimensions overrides applied.
     */
    fun applyOverrides(
        tracks: List<TrackSubscriptionDetails>,
    ): List<TrackSubscriptionDetails> {
        return tracks.mapNotNull { track ->
            val override = trackOverrides[track.session_id] ?: trackOverrides[ALL_PARTICIPANTS]

            if (override == null) {
                track
            } else {
                if (override.visible == false) {
                    null
                } else {
                    override.dimensions?.let { track.copy(dimension = it) } ?: track
                }
            }
        }
    }
}

const val ALL_PARTICIPANTS = "all"
