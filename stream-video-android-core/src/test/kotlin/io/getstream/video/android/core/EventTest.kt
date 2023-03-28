package io.getstream.video.android.core

import io.getstream.video.android.core.events.*
import io.getstream.video.android.core.model.QueryCallsData
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.UserType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventTest : IntegrationTestBase() {
    @Test
    fun `Blocking a user event`() = runTest {

    }

    @Test
    fun `Accepting a call`() = runTest {

    }

    @Test
    fun `Cancelling a call`() = runTest {

    }

    @Test
    fun `Creating a call`() = runTest {

    }

    @Test
    fun `Call ended`() = runTest {

    }

    @Test
    fun testEvent() = runTest {
        val myEvent = ConnectedEvent(clientId = "test123")
        clientImpl.fireEvent(myEvent)
//        when(e) {
//            is BlockedUserEvent -> TODO()
//            is CallAcceptedEvent -> TODO()
//            is CallCancelledEvent -> TODO()
//            is CallCreatedEvent -> TODO()
//            is CallEndedEvent -> TODO()
//            is CallMembersDeletedEvent -> TODO()
//            is CallMembersUpdatedEvent -> TODO()
//            is CallRejectedEvent -> TODO()
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
