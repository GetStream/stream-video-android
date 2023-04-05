package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveSFUSessionTest : IntegrationTestBase() {

    @Test
    fun `Camera operations`() = runTest {
        val call = client.call("default", randomUUID())
        call.camera.devices
        call.camera.flip()
        call.camera.disable()
        call.camera.enable()
        call.camera.status
        //call.camera.select()
    }

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
       // call.state.me.muteAudio()
        //  microphone.devices(), microphone.select(device), microphone.enable(), microphone.disable(), microphone.status() // disabled/enabled/no-permission
        //  speaker.devices(), speaker.select(device), speaker.enable(), speaker.disable(), speaker.status() // disabled/enabled/no-permission
        //  camera.flip(), camera.devices(), camera.select(device), camera.enable(), camera.disable(), camera.status() // disabled/enabled/no-permission

        //call.state.me.muteVideo()
        //call.state.me.flipCamera()

//        call.microphone.devices()
//        call.camera.devices()
//        call.speaker.devices()


        call.state.getParticipant("tommaso")!!.videoTrack
        // TODO: how do we check if you are sending video?
        call.state.getParticipant("tommaso")!!.hasVideo
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