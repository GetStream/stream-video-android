package io.getstream.video.android.core

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveSFUSessionTest : IntegrationTestBase() {

    @Test
    fun camera() = runTest {
        val call = client.call("default", randomUUID())
        val session = call.join()

        // what's the best user experience for:
        // internal we would have call.state.activeSFUSession
        // with the helpers as listed below

        // connection status
//        call.state.isConnected
//
//        // how to get my camera's video feed
//        call.state.me.localVideoStream
//        call.state.getParticipant("tommaso").videoStream
//
//        // how to change my camera
//        call.state.me.flipCamera()
//        call.state.me.selectCamera()
//
//        // muting myself or muting a participant
//        call.state.me.muteAudio()
//        call.state.me.muteVideo()
//        call.state.getParticipant("tommaso").muteAudio()

        // booleans for audio enabled, video enabled, speaker phone enabled

    }

}