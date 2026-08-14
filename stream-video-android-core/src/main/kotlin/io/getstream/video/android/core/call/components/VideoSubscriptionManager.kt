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

import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.call.connection.utils.safeApiCall
import io.getstream.video.android.core.call.utils.TrackOverridesHandler
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.utils.SerialProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse

/**
 * Owns dynascale / video-subscription bookkeeping for one RTC session: track overrides,
 * viewport dimensions, and telling the SFU which remote tracks we want.
 */
internal class VideoSubscriptionManager(
    private val state: CallState,
    private val peerConnections: PeerConnections,
    private val sessionManager: CallSessionManager,
    private val coroutineScope: CoroutineScope,
    private val serialProcessor: SerialProcessor,
    private val sfuConnectionModule: () -> SfuConnectionModule,
) {
    private val logger by taggedLogger("Video:VideoSubscriptionManager")

    val trackOverridesHandler = TrackOverridesHandler(
        onOverridesUpdate = {
            setVideoSubscriptions()
            state._participantVideoEnabledOverrides.value = it.mapValues { entry ->
                entry.value.visible
            }
        },
        logger = logger,
    )

    /**
     * Tells the SFU which video tracks we want to subscribe to
     * - it sends the resolutions we're displaying the video at so the SFU can decide which track to send
     * - when switching SFU we should repeat this info
     * - http calls failing here breaks the call. (since you won't receive the video)
     * - we should retry continuously until it works and after it continues to fail, raise an error that shuts down the call
     * - we retry when:
     * -- error isn't permanent, SFU didn't change, the mute/publish state didn't change
     * -- we cap at 30 retries to prevent endless loops
     */
    fun setVideoSubscriptions(useDefaults: Boolean = false) {
        val participants = state.participants.value
        val remoteParticipants = state.remoteParticipants.value
        coroutineScope.launch {
            serialProcessor.submit("setVideoSubscriptions") {
                peerConnections.subscriber.value?.setVideoSubscriptions(
                    trackOverridesHandler,
                    participants,
                    remoteParticipants,
                    useDefaults,
                )
                Unit
            }
        }
        logger.d { "[setVideoSubscriptions] #sfu; #track; useDefaults: $useDefaults" }
    }

    /**
     * Applies current subscriptions immediately on the subscriber (without going through the
     * serial processor). Used from SFU event handlers that already run on that processor.
     */
    suspend fun applySubscriptionsNow() {
        peerConnections.subscriber.value?.setVideoSubscriptions(
            trackOverridesHandler,
            state.participants.value,
            state.remoteParticipants.value,
        )
    }

    // share what size and which participants we're looking at
    suspend fun updateSubscriptions(
        request: UpdateSubscriptionsRequest,
    ): Result<UpdateSubscriptionsResponse> = safeApiCall {
        logger.v { "[updateSubscriptions] #sfu; #track; request $request" }
        sfuConnectionModule().api.updateSubscriptions(request)
    }

    // sets display track visibility
    @Synchronized
    fun updateTrackDimensions(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimensions: VideoDimension = Subscriber.defaultVideoDimension,
        viewportId: String = sessionId,
    ) {
        logger.v {
            "[updateTrackDimensions] #track; #sfu; sessionId: $sessionId, trackType: $trackType, visible: $visible, dimensions: $dimensions"
        }
        peerConnections.subscriber.value?.setTrackDimension(
            viewportId,
            sessionId,
            trackType,
            visible,
            dimensions,
        )
        coroutineScope.launch {
            serialProcessor.submit("updateTrackDimensions") {
                if (sessionId != sessionManager.sessionId) {
                    // dimension updated for another participant
                    peerConnections.subscriber.value?.setVideoSubscriptions(
                        trackOverridesHandler,
                        state.participants.value,
                        state.remoteParticipants.value,
                    )
                }
                Unit
            }
        }
    }
}
