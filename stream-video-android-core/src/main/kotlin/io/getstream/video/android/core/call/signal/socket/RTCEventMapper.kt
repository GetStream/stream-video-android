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

package io.getstream.video.android.core.call.signal.socket

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.CallEndedSfuEvent
import io.getstream.video.android.core.events.CallGrantsUpdatedEvent
import io.getstream.video.android.core.events.ChangePublishOptionsEvent
import io.getstream.video.android.core.events.ChangePublishQualityEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.ICERestartEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.InboundStateNotificationEvent
import io.getstream.video.android.core.events.InboundStateNotificationEventState
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantCount
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.ParticipantMigrationCompleteEvent
import io.getstream.video.android.core.events.PinUpdate
import io.getstream.video.android.core.events.PinsUpdatedEvent
import io.getstream.video.android.core.events.PublisherAnswerEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.TrackUnpublishedEvent
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.model.UserAudioLevel
import stream.video.sfu.event.SfuEvent

public object RTCEventMapper {
    private val logger by taggedLogger("RTCEventMapper")

    public fun mapEvent(event: SfuEvent): SfuDataEvent {
        return when {
            event.subscriber_offer != null -> SubscriberOfferEvent(event.subscriber_offer.sdp)

            event.connection_quality_changed != null -> with(event.connection_quality_changed) {
                ConnectionQualityChangeEvent(updates = connection_quality_updates)
            }

            event.audio_level_changed != null -> AudioLevelChangedEvent(
                event.audio_level_changed.audio_levels.associate {
                    it.session_id to UserAudioLevel(
                        it.user_id,
                        it.is_speaking,
                        it.level,
                    )
                },
            )

            event.change_publish_quality != null -> ChangePublishQualityEvent(
                event.change_publish_quality,
            )

            event.change_publish_options != null -> ChangePublishOptionsEvent(
                event.change_publish_options,
            )

            event.track_published != null -> with(event.track_published) {
                TrackPublishedEvent(
                    user_id,
                    session_id,
                    type,
                    participant, // Include optional participant for large call optimization
                )
            }

            event.track_unpublished != null -> with(event.track_unpublished) {
                TrackUnpublishedEvent(
                    user_id,
                    session_id,
                    type,
                    participant, // Include optional participant for large call optimization
                )
            }

            event.participant_joined != null -> with(event.participant_joined) {
                ParticipantJoinedEvent(participant!!, call_cid)
            }

            event.participant_left != null -> with(event.participant_left) {
                ParticipantLeftEvent(participant!!, call_cid)
            }

            event.dominant_speaker_changed != null -> DominantSpeakerChangedEvent(
                event.dominant_speaker_changed.user_id,
                event.dominant_speaker_changed.session_id,
            )

            event.health_check_response != null -> SFUHealthCheckEvent(
                ParticipantCount(
                    event.health_check_response.participant_count?.total ?: 0,
                    event.health_check_response.participant_count?.anonymous ?: 0,
                ),
            )

            event.join_response != null -> {
                val counts = ParticipantCount(
                    event.join_response.call_state?.participant_count?.total ?: 0,
                    event.join_response.call_state?.participant_count?.anonymous ?: 0,
                )
                JoinCallResponseEvent(
                    event.join_response.call_state!!,
                    counts,
                    event.join_response.fast_reconnect_deadline_seconds,
                    event.join_response.reconnected,
                    event.join_response.publish_options,
                )
            }

            event.ice_trickle != null -> with(event.ice_trickle) {
                ICETrickleEvent(ice_candidate, peer_type)
            }

            event.ice_restart != null -> ICERestartEvent(event.ice_restart.peer_type)

            event.publisher_answer != null -> PublisherAnswerEvent(sdp = event.publisher_answer.sdp)
            event.error != null -> ErrorEvent(event.error.error, event.error.reconnect_strategy)

            event.call_grants_updated != null -> CallGrantsUpdatedEvent(
                event.call_grants_updated.current_grants,
                event.call_grants_updated.message,
            )

            event.go_away != null -> GoAwayEvent(reason = event.go_away.reason)

            event.participant_migration_complete != null -> ParticipantMigrationCompleteEvent

            event.pins_updated != null -> PinsUpdatedEvent(
                event.pins_updated.pins.map {
                    PinUpdate(it.user_id, it.session_id)
                },
            )

            event.call_ended != null -> {
                CallEndedSfuEvent(event.call_ended.reason.value)
            }

            event.inbound_state_notification != null -> {
                logger.d {
                    "[inbound_state_notification] #sfu; #track; inbound_video_states: ${event.inbound_state_notification.inbound_video_states}"
                }
                InboundStateNotificationEvent(
                    event.inbound_state_notification.inbound_video_states.map {
                        InboundStateNotificationEventState(
                            it.user_id,
                            it.session_id,
                            it.track_type,
                            it.paused,
                        )
                    },
                )
            }

            else -> {
                logger.w { "Unknown event: $event" }
                UnknownEvent(event)
            }
        }
    }
}
