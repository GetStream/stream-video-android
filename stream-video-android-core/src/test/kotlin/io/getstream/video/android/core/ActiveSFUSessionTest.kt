package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
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

//        assertThat(call.state.me.localVideoTrack).isNotNull()
//        assertThat(call.state.me.localAudioTrack).isNotNull()
//
//        assertThat(call.state.connection.value).isEqualTo(ConnectionState.Connected())

        // how to easily expose the has permission stateflow...
        // TODO: lookup from permissions
        // call.state.hasPermission("screenshare")

        // TODO: how to check if audio is disabled/enabled/no-permission
        // call.state.me.audioState()
        call.state.me.muteAudio()
        call.state.me.muteVideo()
        call.state.me.flipCamera()

        call.state.getParticipant("tommaso")!!.videoTrack
        // what's the best user experience for:
        // internal we would have call.state.activeSFUSession
        // with the helpers as listed below

        // connection status
//
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
//
//        call.state.me.muteVideo()
//        call.state.getParticipant("tommaso").muteAudio()

        // booleans for audio enabled, video enabled, speaker phone enabled

    }

}