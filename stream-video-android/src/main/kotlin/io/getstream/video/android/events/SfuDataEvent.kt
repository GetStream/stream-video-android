/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.events

import io.getstream.video.android.model.StreamCallCid
import stream.video.sfu.event.ChangePublishQuality
import stream.video.sfu.models.CallState
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Error
import stream.video.sfu.models.Participant
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoQuality

public sealed class SfuDataEvent

public data class ICETrickleEvent(
    val candidate: String,
    val peerType: PeerType
) : SfuDataEvent()

public data class SubscriberOfferEvent(
    val sdp: String
) : SfuDataEvent()

public data class PublisherAnswerEvent(
    val sdp: String
) : SfuDataEvent()

public data class ConnectionQualityChangeEvent(
    val userId: String,
    val connectionQuality: ConnectionQuality,
) : SfuDataEvent()

public data class AudioLevelChangedEvent(
    val levels: Map<String, Float>
) : SfuDataEvent()

public data class ChangePublishQualityEvent(
    val changePublishQuality: ChangePublishQuality
) : SfuDataEvent()

public data class TrackPublishedEvent(
    val userId: String,
    val sessionId: String,
    val trackType: TrackType
) : SfuDataEvent()

public data class TrackUnpublishedEvent(
    val userId: String,
    val sessionId: String,
    val trackType: TrackType
) : SfuDataEvent()

public data class VideoQualityChangedEvent(
    val qualities: Map<String, VideoQuality>
) : SfuDataEvent()

public data class ParticipantJoinedEvent(
    val participant: Participant,
    val callCid: StreamCallCid,
) : SfuDataEvent()

public data class ParticipantLeftEvent(
    val participant: Participant,
    val callCid: StreamCallCid
) : SfuDataEvent()

public data class DominantSpeakerChangedEvent(
    val userId: String
) : SfuDataEvent()

public object HealthCheckResponseEvent : SfuDataEvent()

public data class JoinCallResponseEvent(
    val callState: CallState
) : SfuDataEvent()

public data class ErrorEvent(val error: Error?) : SfuDataEvent()
