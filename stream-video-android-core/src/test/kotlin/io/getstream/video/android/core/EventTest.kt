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
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.PermissionRequestEvent
import io.getstream.video.android.core.events.RecordingStartedEvent
import io.getstream.video.android.core.events.RecordingStoppedEvent
import io.getstream.video.android.core.events.UpdatedCallPermissionsEvent
import io.getstream.video.android.core.model.CallInfo
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.UserAudioLevel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.OwnCapability
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import java.util.*

@RunWith(RobolectricTestRunner::class)
class EventTest : IntegrationTestBase(connectCoordinatorWS = false) {
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
     * Server TODO:
     * - call id options: not specified, cid, callId, callId as last argument. We should standardize on 1.
     * - we should document the event, the structure of the data, when it's fired and how the SDK handles it
     * - some events provide a list with user ids inside, other events a map<userId, thing>
     * - We should not expose enums like owncapabilities
     *
     * Other Server TODO:
     * - One of the API endpoints returns a list instead of a dict, that's hard to change in the future
     */
    @Test
    fun `test start and stop recording`() = runTest {
        // start by sending the start recording event
        val event = RecordingStartedEvent(callCid = call.cid, cid = call.cid, type = "123")
        clientImpl.fireEvent(event)
        assertThat(call.state.recording.value).isTrue()
        // now stop recording
        val stopRecordingEvent =
            RecordingStoppedEvent(callCid = call.cid, cid = call.cid, type = "123")
        clientImpl.fireEvent(stopRecordingEvent)
        assertThat(call.state.recording.value).isFalse()
    }

    @Test
    fun `Accepting & rejecting a call`() = runTest {
        val acceptedEvent = CallAcceptedEvent(callCid = call.cid, sentByUserId = "123")
        clientImpl.fireEvent(acceptedEvent)
        assertThat(call.state.getParticipant("123")?.acceptedAt?.value).isNotNull()

        val rejectedEvent =
            CallRejectedEvent(callCid = call.cid, user = User(id = "123"), updatedAt = Date())
        clientImpl.fireEvent(rejectedEvent)
        assertThat(call.state.getParticipant("123")?.rejectedAt?.value).isNotNull()
    }

    @Test
    fun `Audio level changes`() = runTest {
        val levels = mutableMapOf("thierry" to UserAudioLevel(true, 10F))
        val event = AudioLevelChangedEvent(levels = levels)
        clientImpl.fireEvent(event, call.cid)

        // ensure we update call data and capabilities
        // TODO: change the map structure
        println(call.state.getParticipant("thierry")?.audioLevel?.value)
    }

    @Test
    fun `Dominant speaker change`() = runTest {
        val event = DominantSpeakerChangedEvent(userId = "jaewoong")
        clientImpl.fireEvent(event, call.cid)

        // ensure we update call data and capabilities
        // TODO: is nesting state objects ok?
        assertThat(call.state.dominantSpeaker.value?.user?.value?.id).isEqualTo("jaewoong")
    }

    @Test
    fun `Network connection quality changes`() = runTest {
        // TODO: this is a list, other events its a map, make up your mind
        val quality = ConnectionQualityInfo(
            user_id = "thierry",
            connection_quality = ConnectionQuality.CONNECTION_QUALITY_EXCELLENT
        )
        val event = ConnectionQualityChangeEvent(updates = mutableListOf(quality))
        clientImpl.fireEvent(event, call.cid)

        assertThat(call.state.getParticipant("thierry")?.connectionQuality?.value).isEqualTo(
            ConnectionQuality.CONNECTION_QUALITY_EXCELLENT
        )
    }

    @Test
    fun `Call updates`() = runTest {
        val capability = OwnCapability.decode("end-call")!!
        val ownCapabilities = mutableListOf<OwnCapability>(capability)
        val custom = mutableMapOf<String, Any>("fruit" to "apple")
        val callInfo = CallInfo(
            call.cid,
            call.type,
            call.id,
            createdByUserId = "thierry",
            false,
            false,
            null,
            Date(),
            custom
        )
        val capabilitiesByRole = mutableMapOf<String, List<String>>(
            "admin" to mutableListOf(
                "end-call",
                "create-call"
            )
        )
        val event = CallUpdatedEvent(
            callCid = call.cid,
            capabilitiesByRole,
            info = callInfo,
            ownCapabilities = ownCapabilities
        )
        clientImpl.fireEvent(event)
        // ensure we update call data and capabilities
        assertThat(call.state.capabilitiesByRole.value).isEqualTo(capabilitiesByRole)
        assertThat(call.state.ownCapabilities.value).isEqualTo(ownCapabilities)
        // TODO: think about custom data assertThat(call.custom).isEqualTo(custom)
    }

    @Test
    fun `Call permissions updated`() {

        // TODO: CID & Type?
        val permissions = mutableListOf<String>("screenshare")
        val requestEvent =
            PermissionRequestEvent(call.cid, "whatsthis", permissions, testData.users["thierry"]!!)
        clientImpl.fireEvent(requestEvent)
        // TODO: How do we show this in the state?
        // TODO: How is this different than call updates?
        // TODO: user field isn't clear, what user?
        val capability = OwnCapability.decode("screenshare")!!
        val ownCapabilities = mutableListOf(capability)
        val permissionsUpdated = UpdatedCallPermissionsEvent(
            call.cid,
            "what",
            ownCapabilities,
            testData.users["thierry"]!!
        )
        clientImpl.fireEvent(permissionsUpdated, call.cid)

        assertThat(call.state.ownCapabilities.value).isEqualTo(ownCapabilities)
    }

    @Test
    fun `Call Ended`() = runTest {
        val call = client.call("default", randomUUID())
        val event = CallEndedEvent(callCid = call.cid, endedByUser = testData.users["thierry"])
        clientImpl.fireEvent(event)

        // TODO: server. you want to know when the call ended and by who.
        // call.state -> endedAt should be set
        assertThat(call.state.endedAt.value).isNotNull()
        assertThat(call.state.endedByUser.value).isEqualTo(testData.users["thierry"])
    }

    @Test
    fun `Participants join and leave`() = runTest {
        val call = client.call("default", randomUUID())
        val participant = Participant(user_id = "thierry", is_speaking = true)
        val joinEvent = ParticipantJoinedEvent(participant = participant, callCid = call.cid)
        clientImpl.fireEvent(joinEvent)
        assertThat(call.state.getParticipant("thierry")!!.speaking.value).isTrue()
        val leaveEvent = ParticipantLeftEvent(participant, callCid = call.cid)
        clientImpl.fireEvent(leaveEvent)
        // TODO: should participants be fully removed or marked as left?
        assertThat(call.state.getParticipant("thierry")).isNull()
    }

    @Test
    fun `Update and delete members`() = runTest {
        val call = client.call("default", randomUUID())
        // TODO: call details/ call info structure is super weird
//        val callUser = CallUser(id="thierry")
//        val callInfo = CallInfo()
//        val callDetails = CallDetails(mutableListOf("thierry"), mutableMapOf("thierry" to callUser), emptyList())
//        val eventUpdated = CallMembersUpdatedEvent(emptyMap(), info=callInfo, details=callDetails)
//        clientImpl.fireEvent(eventUpdated)
        // TODO clean this up val eventDeleted = CallMembersDeletedEvent(user_id="thierry", is_speaking=true)
    }

    @Test
    fun testEvent() = runTest {
        val myEvent = ConnectedEvent(clientId = "test123")
        clientImpl.fireEvent(myEvent)
//        when(e) {
        // TODO: how to treat blocking...
//            is BlockedUserEvent -> TODO()
        //            is UnblockedUserEvent -> TODO()
        // TODO: What's call cancelled? This seems redundant
//            is CallCancelledEvent -> TODO()
        // TODO: decide on how to expose this...
//            is ConnectedEvent -> TODO()
//            is CoordinatorHealthCheckEvent -> TODO() // socket level health check
//            SFUHealthCheckEvent -> TODO()

        // TODO: what does this mean compared to the create and participant joined events?
//            is JoinCallResponseEvent -> TODO()

        // TODO: Webrtc stuff
//            is ICETrickleEvent -> TODO()
//            is PublisherAnswerEvent -> TODO()
//            is SubscriberOfferEvent -> TODO()
//            is TrackPublishedEvent -> TODO()
//            is TrackUnpublishedEvent -> TODO()
//            is ChangePublishQualityEvent -> TODO()
//            is VideoQualityChangedEvent -> TODO()
//            is ErrorEvent -> TODO() // Maybe just console log it for now?
//        }
    }
}
