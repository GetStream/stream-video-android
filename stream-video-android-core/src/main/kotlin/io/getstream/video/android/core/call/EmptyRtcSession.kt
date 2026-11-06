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

import androidx.lifecycle.Lifecycle
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.SfuAnalytics
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.ICERestartResponse

/**
 * An [RtcSession] with no SFU connection behind it, for a call that is joined without WebRTC
 * media.
 *
 * It never creates the SFU connection or any peer connection and leaves the shared peer
 * connection factory untouched. Every public member is a no-op that logs a warning: stats are
 * `null` and ICE restarts fail. [CallSessionManager.setActiveSession] refuses it, since the
 * reconnect, stats and rendering paths read the active session as a live SFU connection.
 */
internal class EmptyRtcSession(
    client: StreamVideo,
    call: Call,
    sessionManager: CallSessionManager,
    sessionId: String,
    apiKey: String,
    lifecycle: Lifecycle,
    sfuAnalytics: SfuAnalytics,
    clientImpl: StreamVideoClient = client as StreamVideoClient,
) : RtcSession(
    client = client,
    powerManager = null,
    call = call,
    sessionManager = sessionManager,
    sessionId = sessionId,
    apiKey = apiKey,
    lifecycle = lifecycle,
    sfuUrl = "",
    sfuWsUrl = "",
    sfuToken = "",
    sfuName = "",
    remoteIceServers = emptyList(),
    clientImpl = clientImpl,
    sfuAnalytics = sfuAnalytics,
    initializeRtc = false,
) {

    private val logger by taggedLogger("Video:EmptyRtcSession")

    override suspend fun reconnect(forceRestart: Boolean) {
        logIgnored("reconnect")
    }

    @Deprecated(
        message = "Use connectInternal() which returns SfuConnectionResult instead of throwing.",
        replaceWith = ReplaceWith("connectInternal(reconnectDetails, options)"),
    )
    override suspend fun connect(
        reconnectDetails: ReconnectDetails?,
        options: List<PublishOption>?,
    ) {
        logIgnored("connect")
    }

    override fun setScreenShareTrack() {
        logIgnored("setScreenShareTrack")
    }

    override fun handleEvent(event: VideoEvent) {
        logIgnored("handleEvent")
    }

    override suspend fun handleIceTrickle(event: ICETrickleEvent) {
        logIgnored("handleIceTrickle")
    }

    override suspend fun handleSubscriberOffer(offerEvent: SubscriberOfferEvent) {
        logIgnored("handleSubscriberOffer")
    }

    override suspend fun getPublisherStats(): RtcStatsReport? {
        logIgnored("getPublisherStats")
        return null
    }

    override suspend fun getSubscriberStats(): RtcStatsReport? {
        logIgnored("getSubscriberStats")
        return null
    }

    override suspend fun requestSubscriberIceRestart(): Result<ICERestartResponse> {
        logIgnored("requestSubscriberIceRestart")
        return noSfuConnection()
    }

    override suspend fun requestPublisherIceRestart(): Result<ICERestartResponse> {
        logIgnored("requestPublisherIceRestart")
        return noSfuConnection()
    }

    override fun updateTrackDimensions(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        dimensions: VideoDimension,
        viewportId: String,
    ) {
        logIgnored("updateTrackDimensions")
    }

    private fun logIgnored(function: String) {
        logger.w { "[$function] Ignored: EmptyRtcSession has no SFU connection" }
    }

    private fun noSfuConnection(): Result<ICERestartResponse> =
        Failure(Error.GenericError("EmptyRtcSession has no SFU connection"))
}
