/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.model.UserAudioLevel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.event.AudioLevel
import stream.video.sfu.models.Participant

@RunWith(RobolectricTestRunner::class)
class EventTest : IntegrationTestBase() {
    /**
     * Every event should update state properly
     * - Coordinator events
     * - SFU events
     *
     * Also review:
     * - CallMetadata
     * - JoinedCall
     * - CallViewModel
     * - Compose lib Call.kt object
     * - StreamVideoStateLauncher
     * - CallClientImpl
     *
     * At the client level
     * - Ringing call
     * - Active call
     *
     * Call level
     * - Call Data and settings
     * - Participants
     * - Various sorting/filtering on participants
     * - Call duration
     * - Own permissions
     *
     * Me & Other participants (members)
     * - Video on/off
     * - Speaking
     * - Network
     *
     * Ringing calls
     * - Normal
     * - Incoming (CallCreatedEvent. Ring=true)
     * - Outgoing, ring=true, and people didn't join yet (member, not a participant)
     * - Rejected/Accept event
     *
     */
    @Test
    fun `test start and stop recording`() = runTest {
        val call = client.call("default", randomUUID())
        // start by sending the start recording event
        val event = RecordingStartedEvent(callCid = call.cid, cid = call.cid, type = "123")
        clientImpl.fireEvent(event)
        assertThat(call.state.recording.value).isTrue()
        // now stop recording
        val stopRecordingEvent = RecordingStoppedEvent(callCid = call.cid, cid = call.cid, type = "123")
        clientImpl.fireEvent(stopRecordingEvent)
        assertThat(call.state.recording.value).isFalse()
    }

    @Test
    fun `Accepting & rejecting a call`() = runTest {
//        val call = client.call("default", randomUUID())
//        val event = CallAcceptedEvent(callCid=call.cid, sentByUserId="123")
//        clientImpl.fireEvent(event)
//
//        val event = CallRejectedEvent(callCid=call.cid, sentByUserId="123")
//        clientImpl.fireEvent(event)

        // call.state -> Member and Participant should have the accepted field updated
    }




    @Test
    fun `Participant Joined`() = runTest {
        // ensure the participant exists
//        val joinEvent = ParticipantJoinedEvent(participant = Participant(user_id = "thierry"),callCid = call.cid)
//        clientImpl.fireEvent(joinEvent, call.cid)
    }

    @Test
    fun `Audio level changes`() = runTest {
        val call = client.call("default", randomUUID())
        val levels = mutableMapOf("thierry" to UserAudioLevel(true, 10F))
        val event = AudioLevelChangedEvent(levels=levels)
        clientImpl.fireEvent(event, call.cid)

        // ensure we update call data and capabilities
        // TODO: change the map structure
        println(call.state.participantMap["thierry"]?.audioLevel?.value)
    }



    @Test
    fun `Call updates`() = runTest {
        val call = client.call("default", randomUUID())
//        val event = CallUpdatedEvent(callCid=call.cid)
//        clientImpl.fireEvent(event)

        // ensure we update call data and capabilities
    }

    @Test
    fun `Creating a call`() = runTest {
        val call = client.call("default", randomUUID())
//        val event = CallCreatedEvent(callCid=call.cid, true, users= emptyMap())
//        clientImpl.fireEvent(event)

        // if the call is ringing it should be added to the client.state.ringingCalls
    }
    @Test
    fun `Call Ended`() = runTest {
        val call = client.call("default", randomUUID())
        val event = CallEndedEvent(callCid=call.cid, endedByUser=testData.users["thierry"])
        clientImpl.fireEvent(event)

        // TODO: server. you want to know when the call ended and by who.
        // call.state -> endedAt should be set
        // call.state.status -> ended
    }

    @Test
    fun testEvent() = runTest {
        val myEvent = ConnectedEvent(clientId = "test123")
        clientImpl.fireEvent(myEvent)
//        when(e) {
//            is BlockedUserEvent -> TODO()
//            is CallCancelledEvent -> TODO()
//            is CallEndedEvent -> TODO()
//            is CallMembersDeletedEvent -> TODO()
//            is CallMembersUpdatedEvent -> TODO()
//            is CallUpdatedEvent -> TODO()
//            is ConnectedEvent -> TODO()
//            is CustomEvent -> TODO()
//            is HealthCheckEvent -> TODO()
//            is PermissionRequestEvent -> TODO()
//            is RecordingStartedEvent -> TODO()
//            is RecordingStoppedEvent -> TODO()
//            is UnblockedUserEvent -> TODO()
//            is UpdatedCallPermissionsEvent -> TODO()
//            is AudioLevelChangedEvent -> TODO()
//            is ChangePublishQualityEvent -> TODO()
//            is ConnectionQualityChangeEvent -> TODO()
//            is DominantSpeakerChangedEvent -> TODO()
//            is ErrorEvent -> TODO()
//            HealthCheckResponseEvent -> TODO()
//            is ICETrickleEvent -> TODO()
//            is JoinCallResponseEvent -> TODO()
//            is ParticipantJoinedEvent -> TODO()
//            is ParticipantLeftEvent -> TODO()
//            is PublisherAnswerEvent -> TODO()
//            is SubscriberOfferEvent -> TODO()
//            is TrackPublishedEvent -> TODO()
//            is TrackUnpublishedEvent -> TODO()
//            is VideoQualityChangedEvent -> TODO()
//        }
    }
}
