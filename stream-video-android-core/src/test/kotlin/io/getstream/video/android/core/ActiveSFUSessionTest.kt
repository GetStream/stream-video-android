package io.getstream.video.android.core

import com.google.common.truth.Truth
import io.getstream.video.android.core.events.SFUConnectedEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class ActiveSFUSessionTest : IntegrationTestBase() {
    /**
     * Creating the publisher and subscriber peer connection
     * - If we are not allowed to publish, we don't need to establish that peer connection
     *
     */

    @Test
    fun `Publish the video feed`() = runTest {
        val joinResult = call.join()
        assertSuccess(joinResult)
        waitForNextEvent<SFUConnectedEvent>()
        Truth.assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected)

        call.activeSession?.publishVideo()
    }

    @Test
    fun `Create a subscriber peer connection`() = runTest {

    }

    @Test
    fun `Create a publisher peer connection`() = runTest {

    }

    @Test
    fun `Create an SDP`() = runTest {

    }

    @Test
    fun `ICE Trickle`() = runTest {

    }

    @Test
    fun `onNegotiationNeeded`() = runTest {

    }

    @Test
    fun `Dynascale events`() = runTest {

    }

    @Test
    fun `Send audio track`() = runTest {

    }

    @Test
    fun `Send video track`() = runTest {

    }

    @Test
    fun `Switch video track`() = runTest {

    }


}