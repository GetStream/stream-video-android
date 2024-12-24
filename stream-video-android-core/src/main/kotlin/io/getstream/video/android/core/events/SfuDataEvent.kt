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

package io.getstream.video.android.core.events

import io.getstream.video.android.model.UserAudioLevel
import org.openapitools.client.models.VideoEvent
import stream.video.sfu.event.ChangePublishOptions
import stream.video.sfu.event.ChangePublishQuality
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.event.SfuRequest
import stream.video.sfu.models.CallGrants
import stream.video.sfu.models.CallState
import stream.video.sfu.models.Error
import stream.video.sfu.models.GoAwayReason
import stream.video.sfu.models.Participant
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.WebsocketReconnectStrategy

public sealed class SfuDataEvent : VideoEvent() {
    override fun getEventType(): String {
        return this::class.simpleName ?: "UnknownEvent"
    }
}

public data class SfuDataRequest(val sfuRequest: SfuRequest) : SfuDataEvent() {
    override fun getEventType(): String {
        return "SfuDataRequest"
    }
}

public data class PublisherAnswerEvent(
    val sdp: String,
) : SfuDataEvent()

public data class GoAwayEvent(
    val reason: GoAwayReason,
) : SfuDataEvent()

public data class CallGrantsUpdatedEvent(
    val current_grants: CallGrants?,
    val message: String = "",
) : SfuDataEvent()

public data class SFUConnectedEvent(
    val clientId: String,
) : SfuDataEvent()

public data class ICETrickleEvent(
    val candidate: String,
    val peerType: PeerType,
) : SfuDataEvent()

public data class ICERestartEvent(
    val peerType: PeerType,
) : SfuDataEvent()

public data class SubscriberOfferEvent(
    val sdp: String,
) : SfuDataEvent()

public data class ConnectionQualityChangeEvent(
    val updates: List<ConnectionQualityInfo>,
) : SfuDataEvent()

public data class AudioLevelChangedEvent(
    val levels: Map<String, UserAudioLevel>,
) : SfuDataEvent()

public data class ChangePublishQualityEvent(
    val changePublishQuality: ChangePublishQuality,
) : SfuDataEvent()

public data class ChangePublishOptionsEvent(
    val change: ChangePublishOptions,
) : SfuDataEvent()

public data class TrackPublishedEvent(
    val userId: String,
    val sessionId: String,
    val trackType: TrackType,
) : SfuDataEvent()

public data class TrackUnpublishedEvent(
    val userId: String,
    val sessionId: String,
    val trackType: TrackType,
) : SfuDataEvent()

public data class ParticipantJoinedEvent(

    val participant: Participant,
    val callCid: String,
) : SfuDataEvent()

public data class ParticipantLeftEvent(
    val participant: Participant,
    val callCid: String,
) : SfuDataEvent()

public data class DominantSpeakerChangedEvent(
    val userId: String,
    val sessionId: String,
) : SfuDataEvent()

public data class ParticipantCount(
    val total: Int,
    val anonymous: Int,
)

public data class SFUHealthCheckEvent(
    val participantCount: ParticipantCount,
) : SfuDataEvent()

public data class JoinCallResponseEvent(
    val callState: CallState,
    val participantCount: ParticipantCount,
    val fastReconnectDeadlineSeconds: Int,
    val isReconnected: Boolean,
    val publishOptions: List<PublishOption>,
) : SfuDataEvent()

public data class CallEndedSfuEvent(
    val reason: Int,
) : SfuDataEvent()

public data object ParticipantMigrationCompleteEvent : SfuDataEvent()

public data class PinsUpdatedEvent(
    val pins: List<PinUpdate>,
) : SfuDataEvent()

public data class PinUpdate(val userId: String, val sessionId: String)

public data class UnknownEvent(val event: Any?) : SfuDataEvent()

public data class ErrorEvent(val error: Error?, val reconnectStrategy: WebsocketReconnectStrategy) :
    SfuDataEvent()

public class SfuSocketError(val error: Error?) : Throwable() {
    override val message: String?
        get() = error?.message
}
