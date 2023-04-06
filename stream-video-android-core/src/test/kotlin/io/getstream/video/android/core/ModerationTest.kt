package io.getstream.video.android.core

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModerationTest: IntegrationTestBase() {
    @Test
    fun `Edtech style moderation`() = runTest {

        // a teacher mutes the audio for a student
        val result = call.state.getOrCreateParticipant("tommaso").muteAudio()
        assertSuccess(result)

        // this needs to fire an event that update's Tommaso's permissions...
        TODO()

        // the student is not able to unmute themselves
        // so state for the microphone is on/off/not allowed
        call.microphone.status // on/off/no permission

        // they can request to unmute though
        call.requestPermissions("unmute")

        // the teacher grants this request
        TODO()

        // again, an event should update permissions

        // and the student can click unmute
        // same applies for audio, video and screen sharing
    }
    @Test
    fun `Zoom style moderation`() = runTest {
        // on a large call someone will keep background noise going
        // you want to mute everyone at once
        val result = call.muteAllUsers()
        assertSuccess(result)

        // people can unmute themselves
        TODO()
    }
}