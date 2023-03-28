package io.getstream.video.android.core

import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.VideoEvent
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
    fun testEvent() = runTest {
        val myEvent = ConnectedEvent(clientId = "test123")
        clientImpl.fireEvent(myEvent)
    }

}
