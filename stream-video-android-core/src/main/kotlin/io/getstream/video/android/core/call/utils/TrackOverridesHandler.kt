package io.getstream.video.android.core.call.utils

import io.getstream.log.TaggedLogger
import io.getstream.video.android.core.ParticipantState
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.TrackSubscriptionDetails

/**
 * Handles incoming video track overrides (resolution and visibility).
 *
 * @param getParticipantList Lambda used to get the list of call participants.
 * @param getParticipant Lambda used to get a call participant by session ID. Used to take advantage of a potential O(1) participant lookup, e.g. in a HashMap.
 * @param onOverridesUpdate Lambda used to notify the caller when the overrides are updated.
 * @param logger Logger to be used.
 */
internal class TrackOverridesHandler(
    private val getParticipantList: () -> List<ParticipantState>,
    private val getParticipant: (sessionId: String) -> ParticipantState?,
    private val onOverridesUpdate: (overrides: Map<String, TrackOverride>) -> Unit,
    private val logger: TaggedLogger? = null,
) {

    private val trackOverrides: MutableMap<String, TrackOverride> = mutableMapOf()

    internal data class TrackOverride(
        val dimensions: VideoDimension? = null,
        val visible: Boolean? = null,
    )

    /**
     * Updates incoming video dimensions overrides.
     *
     * @param sessionIds List of session IDs to update. If `null`, the override will be applied to all participants.
     * @param dimensions Video dimensions to set. Set to `null` to switch back to auto.
     */
    internal fun updateOverrides(
        sessionIds: List<String>? = null,
        dimensions: VideoDimension? = null,
    ) {
        val putDimensions = dimensions

        if (sessionIds == null) {
            putOrRemoveDimensionsOverride(
                sessionId = ALL_PARTICIPANTS,
                putDimensions = putDimensions,
                existingVisibility = trackOverrides[ALL_PARTICIPANTS]?.visible
            )
        } else {
            sessionIds.forEach { sessionId ->
                putOrRemoveDimensionsOverride(
                    sessionId = sessionId,
                    putDimensions = putDimensions,
                    existingVisibility = trackOverrides[sessionId]?.visible
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
            trackOverrides.put(sessionId, TrackOverride(dimensions = putDimensions, visible = existingVisibility))
        }
    }

    /**
     * Updates incoming video visibility overrides.
     *
     * @param sessionIds List of session IDs to update. If `null`, the override will be applied to all participants.
     * @param visible Video visibility to set. Set to `null` to switch back to auto.
     */
    internal fun updateOverrides(
        sessionIds: List<String>? = null,
        visible: Boolean? = null,
    ) {
        val putVisibility = visible

        if (sessionIds == null) {
            putOrRemoveVisibilityOverride(
                sessionId = ALL_PARTICIPANTS,
                putVisibility = putVisibility,
                existingDimensions = trackOverrides[ALL_PARTICIPANTS]?.dimensions
            )

            getParticipantList().forEach {
                val override = applyOverrides(it.sessionId, true)
                it._videoEnabled.value = override
            }
        } else {
            sessionIds.forEach { sessionId ->
                putOrRemoveVisibilityOverride(
                    sessionId = sessionId,
                    putVisibility = putVisibility,
                    existingDimensions = trackOverrides[sessionId]?.dimensions
                )

                getParticipant(sessionId)?.let {
                    val override = applyOverrides(it.sessionId, true)
                    it._videoEnabled.value = override
                }
            }
        }

        onOverridesUpdate(trackOverrides)

        logger?.d { "[updateOverrides] #manual-quality-selection; Overrides: $trackOverrides" }
    }

    private fun putOrRemoveVisibilityOverride(sessionId: String, putVisibility: Boolean?, existingDimensions: VideoDimension?) {
        if (existingDimensions == null && (putVisibility == true || putVisibility == null)) {
            trackOverrides.remove(sessionId)
        } else {
            trackOverrides.put(sessionId, TrackOverride(dimensions = existingDimensions, visible = putVisibility))
        }
    }

    /**
     * Applies overrides to the given participant's video enabled property.
     *
     * @return The overridden video enabled property or the original value if no override is found.
     */
    internal fun applyOverrides(sessionId: String, videoEnabledFallback: Boolean): Boolean {
        val override = trackOverrides[sessionId] ?: trackOverrides[ALL_PARTICIPANTS]

        return if (override?.visible == false) {
            false
        } else {
            videoEnabledFallback
        }
    }


    /**
     * Applies overrides to given list of tracks.
     *
     * @return List of tracks with visibility and dimensions overrides applied.
     */
    internal fun applyOverrides(tracks: List<TrackSubscriptionDetails>): List<TrackSubscriptionDetails> {
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

private const val ALL_PARTICIPANTS = "all"