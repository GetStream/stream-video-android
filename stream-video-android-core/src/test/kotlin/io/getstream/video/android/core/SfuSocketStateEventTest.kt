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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.BlockedUserEvent
import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.CallReactionEvent
import io.getstream.android.video.generated.models.CallRecordingStartedEvent
import io.getstream.android.video.generated.models.CallRecordingStoppedEvent
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.PermissionRequestEvent
import io.getstream.android.video.generated.models.ReactionResponse
import io.getstream.android.video.generated.models.UnblockedUserEvent
import io.getstream.android.video.generated.models.UpdatedCallPermissionsEvent
import io.getstream.android.video.generated.models.UserResponse
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.base.toResponse
import io.getstream.video.android.core.events.AudioLevelChangedEvent
import io.getstream.video.android.core.events.ConnectionQualityChangeEvent
import io.getstream.video.android.core.events.DominantSpeakerChangedEvent
import io.getstream.video.android.core.events.ParticipantJoinedEvent
import io.getstream.video.android.core.events.ParticipantLeftEvent
import io.getstream.video.android.core.events.TrackPublishedEvent
import io.getstream.video.android.core.model.NetworkQuality
import io.getstream.video.android.core.permission.PermissionRequest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.event.ConnectionQualityInfo
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType

@RunWith(RobolectricTestRunner::class)
class SfuSocketStateEventTest : IntegrationTestBase(connectCoordinatorWS = false) {
    @Test
    fun `test start and stop composite recording`() = runTest {
        val startEvent = CallRecordingStartedEvent(
            callCid = call.cid,
            nowUtc,
            "",
            CallRecordingStartedEvent.RecordingType.Composite,
            "call.recording_started",
        )
        clientImpl.fireEvent(startEvent)
        assertThat(call.state.recording.value).isTrue()
        assertThat(call.state.compositeRecording.value).isTrue()

        val stopEvent = CallRecordingStoppedEvent(
            callCid = call.cid,
            nowUtc,
            "",
            CallRecordingStoppedEvent.RecordingType.Composite,
            "call.recording_stopped",
        )
        clientImpl.fireEvent(stopEvent)
        assertThat(call.state.recording.value).isFalse()
        assertThat(call.state.compositeRecording.value).isFalse()
    }

    @Test
    fun `test start and stop individual recording`() = runTest {
        val startEvent = CallRecordingStartedEvent(
            callCid = call.cid,
            nowUtc,
            "",
            CallRecordingStartedEvent.RecordingType.Individual,
            "call.recording_started",
        )
        clientImpl.fireEvent(startEvent)
        assertThat(call.state.recording.value).isTrue()
        assertThat(call.state.individualRecording.value).isTrue()

        val stopEvent = CallRecordingStoppedEvent(
            callCid = call.cid,
            nowUtc,
            "",
            CallRecordingStoppedEvent.RecordingType.Individual,
            "call.recording_stopped",
        )
        clientImpl.fireEvent(stopEvent)
        assertThat(call.state.recording.value).isFalse()
        assertThat(call.state.individualRecording.value).isFalse()
    }

    @Test
    fun `test start and stop raw recording`() = runTest {
        val startEvent = CallRecordingStartedEvent(
            callCid = call.cid,
            nowUtc,
            "",
            CallRecordingStartedEvent.RecordingType.Raw,
            "call.recording_started",
        )
        clientImpl.fireEvent(startEvent)
        assertThat(call.state.recording.value).isTrue()
        assertThat(call.state.rawRecording.value).isTrue()

        val stopEvent = CallRecordingStoppedEvent(
            callCid = call.cid,
            nowUtc,
            "",
            CallRecordingStoppedEvent.RecordingType.Raw,
            "call.recording_stopped",
        )
        clientImpl.fireEvent(stopEvent)
        assertThat(call.state.recording.value).isFalse()
        assertThat(call.state.rawRecording.value).isFalse()
    }

    @Test
    fun `test recording types are independent of each other`() = runTest {
        // Start all three recording types
        clientImpl.fireEvent(
            CallRecordingStartedEvent(
                call.cid,
                nowUtc,
                "",
                CallRecordingStartedEvent.RecordingType.Composite,
                "call.recording_started",
            ),
        )
        clientImpl.fireEvent(
            CallRecordingStartedEvent(
                call.cid,
                nowUtc,
                "",
                CallRecordingStartedEvent.RecordingType.Individual,
                "call.recording_started",
            ),
        )
        clientImpl.fireEvent(
            CallRecordingStartedEvent(
                call.cid,
                nowUtc,
                "",
                CallRecordingStartedEvent.RecordingType.Raw,
                "call.recording_started",
            ),
        )

        assertThat(call.state.compositeRecording.value).isTrue()
        assertThat(call.state.individualRecording.value).isTrue()
        assertThat(call.state.rawRecording.value).isTrue()

        // Stop only composite — other types should remain true
        clientImpl.fireEvent(
            CallRecordingStoppedEvent(
                call.cid,
                nowUtc,
                "",
                CallRecordingStoppedEvent.RecordingType.Composite,
                "call.recording_stopped",
            ),
        )

        assertThat(call.state.compositeRecording.value).isFalse()
        assertThat(call.state.individualRecording.value).isTrue()
        assertThat(call.state.rawRecording.value).isTrue()
    }

    @Test
    fun `Audio level changes`() = runTest {
        // ensure the participant exists
        call.state.getOrCreateParticipant("thierry", "thierry", updateFlow = true)

        val levels = mutableMapOf(
            "thierry" to io.getstream.video.android.model.UserAudioLevel(
                "thierry",
                true,
                10F,
            ),
        )
        val event = AudioLevelChangedEvent(levels = levels)
        clientImpl.fireEvent(event, call.cid)

        // ensure we update call data and capabilities
        assertThat(
            call.state.activeSpeakers.value.map { it.userId.value },
        ).containsExactly("thierry")
    }

    @Test
    fun `Dominant speaker change`() = runTest {
        val event = DominantSpeakerChangedEvent(userId = "jaewoong", sessionId = "jaewoong")
        clientImpl.fireEvent(event, call.cid)

        // ensure we update call data and capabilities
        assertThat(call.state.dominantSpeaker.value?.userId?.value).isEqualTo("jaewoong")
    }

    @Test
    fun `Network connection quality changes`() = runTest {
        // ensure we have a participant setup
        val thierry = Participant(user_id = "thierry", is_speaking = true, session_id = "thierry")
        call.state.getOrCreateParticipant(thierry)
        // send the event
        val quality = ConnectionQualityInfo(
            session_id = "thierry",
            user_id = "thierry",
            connection_quality = ConnectionQuality.CONNECTION_QUALITY_EXCELLENT,
        )
        val event = ConnectionQualityChangeEvent(updates = mutableListOf(quality))
        clientImpl.fireEvent(event, call.cid)

        assertThat(
            call.state.getParticipantBySessionId("thierry")?.networkQuality?.value,
        ).isEqualTo(
            NetworkQuality.Excellent(),
        )
    }

    @Test
    fun `Call updates`() = runTest {
        val capability = OwnCapability.EndCall
        val ownCapabilities = mutableListOf<OwnCapability>(capability)
        val custom = mutableMapOf<String, Any>("fruit" to "apple")
//        val callInfo = CallInfo(
//            call.cid,
//            call.type,
//            call.id,
//            createdByUserId = "thierry",
//            false,
//            false,
//            null,
//            Date(),
//            custom
//        )
//        val capabilitiesByRole = mutableMapOf<String, List<String>>(
//            "admin" to mutableListOf(
//                "end-call",
//                "create-call"
//            )
//        )
//        val event = CallUpdatedEvent(
//            call = call.toResponse(),
//            callCid = call.cid,
//            capabilitiesByRole =capabilitiesByRole,
//            createdAt = nowUtc,
//            type = "call.ended",
//        )
//        clientImpl.fireEvent(event)
//        // ensure we update call data and capabilities
//        assertThat(call.state.capabilitiesByRole.value).isEqualTo(capabilitiesByRole)
//        assertThat(call.state.ownCapabilities.value).isEqualTo(ownCapabilities)
//        // TODO: think about custom data assertThat(call.custom).isEqualTo(custom)
    }

    @Test
    fun `Call permissions updated`() {
        val permissions = mutableListOf<String>("screenshare")
        val requestEvent = PermissionRequestEvent(
            callCid = call.cid,
            createdAt = nowUtc,
            permissions = permissions,
            type = "call.permission_request",
            user = testData.users["thierry"]!!.toUserResponse(),
        )
        clientImpl.fireEvent(requestEvent)
        val capability = OwnCapability.Screenshare
        val ownCapabilities = mutableListOf(capability)
        val permissionsUpdated = UpdatedCallPermissionsEvent(
            call.cid,
            nowUtc,
            ownCapabilities,
            type = "call.permissions_updated",
            user = testData.users["thierry"]!!.toUserResponse(),
        )
        clientImpl.fireEvent(permissionsUpdated, call.cid)

        assertThat(call.state.ownCapabilities.value).isEqualTo(ownCapabilities)
    }

    @Test
    fun `Call Ended`() = runTest {
        val call = client.call("default", randomUUID())
        val userResponse = testData.users["thierry"]!!.toUserResponse()
        val event = CallEndedEvent(
            call = call.toResponse(userResponse),
            createdAt = nowUtc,
            callCid = call.cid,
            type = "call.ended",
            user = userResponse,
        )

        clientImpl.fireEvent(event)

        // TODO: server. you want to know when the call ended and by who.
        // call.state -> endedAt should be set
        assertThat(call.state.endedAt.value).isNotNull()
        assertThat(call.state.endedByUser.value).isEqualTo(testData.users["thierry"])
    }

    @Test
    fun `Participants join and leave`() = runTest {
        val call = client.call("default", randomUUID())
        val participant =
            Participant(user_id = "thierry", is_speaking = true, session_id = "thierry")
        val joinEvent = ParticipantJoinedEvent(participant = participant, callCid = call.cid)
        clientImpl.fireEvent(joinEvent, call.cid)
        assertThat(call.state.getParticipantBySessionId("thierry")!!.speaking.value).isTrue()
        val leaveEvent = ParticipantLeftEvent(participant, callCid = call.cid)
        clientImpl.fireEvent(leaveEvent, call.cid)
        assertThat(call.state.getParticipantBySessionId("thierry")).isNull()
    }

    @Test
    fun `Block and unblock a user`() = runTest {
        val blockEvent = BlockedUserEvent(
            callCid = call.cid,
            createdAt = nowUtc,
            type = "call.blocked_user",
            user = testData.users["thierry"]!!.toUserResponse(),
        )
        clientImpl.fireEvent(blockEvent)
        assertThat(call.state.blockedUsers.value).contains("thierry")

        val unBlockEvent = UnblockedUserEvent(
            callCid = call.cid,
            createdAt = nowUtc,
            type = "call.blocked_user",
            user = testData.users["thierry"]!!.toUserResponse(),
        )
        clientImpl.fireEvent(unBlockEvent)
        assertThat(call.state.blockedUsers.value).doesNotContain("thierry")
    }

    @Test
    fun `Permission request event`() = runTest {
        val permissionRequestEvent = PermissionRequestEvent(
            callCid = call.cid,
            createdAt = nowUtc,
            type = "call.permission_request",
            permissions = mutableListOf("screenshare"),
            user = testData.users["thierry"]!!.toUserResponse(),
        )
        val permissionRequest = PermissionRequest(
            call,
            permissionRequestEvent,
        )
        clientImpl.fireEvent(permissionRequestEvent)
        assertThat(call.state.permissionRequests.value).contains(
            permissionRequest,
        )
    }

    @Test
    fun `Call member permissions updated`() = runTest {
        // TODO: Implement call to response
//        val event = CallMemberUpdatedPermissionEvent(
//            callCid=call.cid,
//            createdAt=nowUtc,
//            type="call.updated_permission",
//            capabilitiesByRole = mutableMapOf("admin" to mutableListOf("end-call", "create-call")),
//        )
//        clientImpl.fireEvent(event)
//        assertThat(call.state.capabilitiesByRole.value).isEqualTo(event.capabilitiesByRole)
    }

    @Test
    fun `Reaction event`() = runTest {
        val reactionEvent = CallReactionEvent(
            callCid = call.cid,
            createdAt = nowUtc,
            type = "call.reaction",
            reaction = ReactionResponse(
                type = "like",
                user = testData.users["thierry"]!!.toUserResponse(),
                custom = mutableMapOf("fruit" to "apple"),
            ),
        )
        // ensure the participant is setup
        val thierry = Participant(user_id = "thierry", is_speaking = true, session_id = "thierry")
        call.state.getOrCreateParticipant(thierry)
        clientImpl.fireEvent(reactionEvent)
        // reactions are sometimes shown on the given participant's UI
        val participant = call.state.getParticipantBySessionId("thierry")
        assertThat(participant!!.reactions.value.map { it.response.type }).contains("like")

        // other times they will be show on the main call UI
        assertThat(call.state.reactions.value.map { it.type }).contains("like")
    }

    @Test
    fun `test screenshare stops when participant disconnects`() = runTest {
        // create a call
        val sessionId = randomUUID()
        val call = client.call("default", sessionId)
        val participant =
            Participant(user_id = "thierry", is_speaking = true, session_id = sessionId)

        // participant joins
        val joinEvent = ParticipantJoinedEvent(participant = participant, callCid = call.cid)
        clientImpl.fireEvent(joinEvent, call.cid)

        // participant shares screens
        val screenShareEvent = TrackPublishedEvent(
            userId = "thierry",
            sessionId = sessionId,
            trackType = TrackType.TRACK_TYPE_SCREEN_SHARE,
        )
        clientImpl.fireEvent(screenShareEvent, call.cid)

        // participant leaves
        val leaveEvent = ParticipantLeftEvent(participant, callCid = call.cid)
        clientImpl.fireEvent(leaveEvent, call.cid)

        // verify that the screen-sharing session is null
        assertThat(call.state.screenSharingSession.value).isNull()
    }
}

private fun io.getstream.video.android.model.User.toUserResponse(): UserResponse {
    return UserResponse(
        id = id,
        role = role ?: "user",
        teams = teams ?: emptyList(),
        image = image,
        name = name,
        custom = custom ?: emptyMap(),
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
        deletedAt = OffsetDateTime.now(),
        // TODO: implement these
        blockedUserIds = emptyList(),
        language = "",
    )
}
