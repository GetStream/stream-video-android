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

import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.GoAwayEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.PinsUpdatedEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.events.UnknownEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.event.AudioLevel
import stream.video.sfu.event.AudioLevelChanged
import stream.video.sfu.event.ConnectionQualityChanged
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.event.GoAway
import stream.video.sfu.event.ParticipantJoined
import stream.video.sfu.event.PinsChanged
import stream.video.sfu.event.SfuEvent
import stream.video.sfu.event.SubscriberOffer
import stream.video.sfu.event.TrackPublished
import stream.video.sfu.models.GoAwayReason
import stream.video.sfu.models.Participant
import stream.video.sfu.models.Pin
import stream.video.sfu.models.TrackType
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class RTCEventMapperTest {

    @Test
    fun `maps subscriber_offer event`() {
        val sfuEvent = SfuEvent(subscriber_offer = SubscriberOffer(sdp = "fake-sdp"))

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<SubscriberOfferEvent>(result)
        assertEquals("fake-sdp", result.sdp)
    }

    @Test
    fun `maps connection_quality_changed event`() {
        val list = emptyList<ConnectionQualityInfo>()
        val update = ConnectionQualityChangeEvent(list)
        val sfuEvent = SfuEvent(
            connection_quality_changed = ConnectionQualityChanged(
                connection_quality_updates = list,
            ),
        )

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<ConnectionQualityChangeEvent>(result)
        assertEquals(list, result.updates)
    }

    @Test
    fun `maps audio_level_changed event`() {
        val audioLevel = AudioLevel("user1", "session1", 0.8f, true)
        val sfuEvent = SfuEvent(
            audio_level_changed = AudioLevelChanged(audio_levels = listOf(audioLevel)),
        )

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<AudioLevelChangedEvent>(result)
        val mapped = result.levels["session1"]!!
        assertEquals("user1", mapped.userId)
        assertEquals(true, mapped.isSpeaking)
        assertEquals(0.8f, mapped.audioLevel)
    }

    @Test
    fun `maps track_published event`() {
        val dto = TrackPublished("user1", "session1", TrackType.TRACK_TYPE_VIDEO)
        val sfuEvent = SfuEvent(track_published = dto)

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<TrackPublishedEvent>(result)
        assertEquals("user1", result.userId)
        assertEquals("session1", result.sessionId)
        assertEquals(TrackType.TRACK_TYPE_VIDEO, result.trackType)
    }

    @Test
    fun `maps participant_joined event`() {
        val participant = Participant("user1")
        val dto = ParticipantJoined(participant = participant, call_cid = "cid123")
        val sfuEvent = SfuEvent(participant_joined = dto)

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<ParticipantJoinedEvent>(result)
        assertEquals(participant, result.participant)
        assertEquals("cid123", result.callCid)
    }

    @Test
    fun `maps go_away event`() {
        val sfuEvent = SfuEvent(go_away = GoAway(reason = GoAwayReason.GO_AWAY_REASON_UNSPECIFIED))

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<GoAwayEvent>(result)
        assertEquals(GoAwayReason.GO_AWAY_REASON_UNSPECIFIED, result.reason)
    }

    @Test
    fun `maps pins_updated event`() {
        val pin = Pin("user1", "session1")
        val sfuEvent = SfuEvent(pins_updated = PinsChanged(listOf(pin)))

        val result = RTCEventMapper.mapEvent(sfuEvent)

        assertIs<PinsUpdatedEvent>(result)
        assertEquals("user1", result.pins.first().userId)
    }

    @Test
    fun `maps unknown event`() {
        val emptyEvent = SfuEvent()

        val result = RTCEventMapper.mapEvent(emptyEvent)

        assertIs<UnknownEvent>(result)
        assertEquals(emptyEvent, result.event)
    }
}
