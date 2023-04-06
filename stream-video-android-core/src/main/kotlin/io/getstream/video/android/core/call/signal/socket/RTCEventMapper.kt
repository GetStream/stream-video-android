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

package io.getstream.video.android.core.call.signal.socket

import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.model.UserAudioLevel
import stream.video.sfu.event.SfuEvent

public object RTCEventMapper {

    public fun mapEvent(event: SfuEvent): SfuDataEvent {
        return when {
            event.subscriber_offer != null -> SubscriberOfferEvent(event.subscriber_offer.sdp)
            event.publisher_answer != null -> with(event.publisher_answer) {
                PublisherAnswerEvent(sdp)
            }

            event.connection_quality_changed != null -> with(event.connection_quality_changed) {
                ConnectionQualityChangeEvent(updates = connection_quality_updates)
            }
            event.audio_level_changed != null -> AudioLevelChangedEvent(
                event.audio_level_changed.audio_levels.associate {
                    it.session_id to UserAudioLevel(
                        it.user_id,
                        it.is_speaking,
                        it.level
                    )
                }
            )
            event.change_publish_quality != null -> ChangePublishQualityEvent(event.change_publish_quality)

            event.track_published != null -> with(event.track_published) {
                TrackPublishedEvent(
                    user_id,
                    session_id,
                    type
                )
            }

            event.track_unpublished != null -> with(event.track_unpublished) {
                TrackUnpublishedEvent(
                    user_id,
                    session_id,
                    type
                )
            }

            event.participant_joined != null -> with(event.participant_joined) {
                ParticipantJoinedEvent(participant!!, call_cid)
            }
            event.participant_left != null -> with(event.participant_left) {
                ParticipantLeftEvent(participant!!, call_cid)
            }
            event.dominant_speaker_changed != null -> DominantSpeakerChangedEvent(event.dominant_speaker_changed.user_id, event.dominant_speaker_changed.session_id)
            event.health_check_response != null -> SFUHealthCheckEvent
            event.join_response != null -> with(event.join_response) {
                JoinCallResponseEvent(call_state!!)
            }
            event.ice_trickle != null -> with(event.ice_trickle) {
                ICETrickleEvent(ice_candidate, peer_type)
            }
            event.error != null -> ErrorEvent(event.error.error)

            else -> throw IllegalStateException("Unknown event")
        }
    }
}
