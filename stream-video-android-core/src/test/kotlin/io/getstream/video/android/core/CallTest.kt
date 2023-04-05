package io.getstream.video.android.core

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallTest: IntegrationTestBase() {

    /**
     * Muting is tricky
     * - you can mute yourself
     * - a moderator can mute you (receive an event)
     *
     * Updating state and actual audio should be in 1 place
     * So they can't end up in different states
     *
     *
     */

    @Test
    fun `Muting yourself`() = runTest {
        val call = client.call("default", randomUUID())
        val me = call.state.me.value

        call.camera.devices
        call.camera.flip() // this needs to update the call state though..
        call.camera.disable()
        call.camera.enable()
        call.camera.status

        val tommaso = call.state.getParticipant("tommaso")
        tommaso!!.muteAudio() // send a moderation request to mute this person

        //call.camera.select()
    }
}