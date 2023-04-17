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

package io.getstream.video.android.core.stories

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.openapitools.client.models.BlockedUserEvent
import org.openapitools.client.models.CallMemberRemovedEvent
import org.openapitools.client.models.CallMemberUpdatedPermissionEvent
import org.openapitools.client.models.PermissionRequestEvent
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
/**
 *   https://www.notion.so/stream-wiki/Moderation-Permissions-for-video-37a3376268654095b9aafaba12d4bb69
 *   https://www.notion.so/stream-wiki/Call-Permissions-832f914ad4c545cf8f048012900ad21d
 */
class ModerationTest : IntegrationTestBase() {

    @Test
    fun `Basic moderation - Block a user`() = runTest {
        val response = call.blockUser("tommaso")
        assertSuccess(response)
        val event = waitForNextEvent<BlockedUserEvent>()
        assertThat(event.user.id).isEqualTo("tommaso")
    }

    @Test
    fun `Basic moderation - Remove a member`() = runTest {
        val response = call.removeMembers(listOf("tommaso"))
        assertSuccess(response)
        val event = waitForNextEvent<CallMemberRemovedEvent>()
        assertThat(event.members).contains("tommaso")
    }

    @Test
    fun `Basic moderation - Request permission to share your screen`() = runTest {

        val hasPermission = call.state.hasPermission("screenshare").value
        assertThat(hasPermission).isFalse()

        val response = call.requestPermissions("screenshare")
        assertSuccess(response)
        waitForNextEvent<PermissionRequestEvent>().also {
            assertThat(it.user.id).isEqualTo(client.user.id)
            assertThat(it.permissions).contains("screenshare")
        }

        val updatePermissionResponse = call.grantPermissions("thierry", listOf("screenshare"))
        assertSuccess(updatePermissionResponse)
        val removePermissionResult = call.revokePermissions("thierry", listOf("screenshare"))
        assertSuccess(removePermissionResult)

        waitForNextEvent<CallMemberUpdatedPermissionEvent>().also {
        }
    }

    @Test
    fun `Basic moderation - Call settings`() = runTest {
        // see if we are allowed to request this permission.
        // the way this works is a bit weird, since i'd like to see if i
        // canRequest("screenshare"), hasPermission("screenshare"), grantPermission("screenshare")
        val settings = call.state.settings.value
        assertThat(settings!!.audio.accessRequestEnabled).isTrue()
    }

    @Test
    fun `Basic moderation - Mute everyone other than yourself`() = runTest {
        val result = call.muteAllUsers()
        assertSuccess(result)
    }

    @Test
    fun `Basic moderation - Mute a specific person`() = runTest {
        // TODO: what's the behaviour for muting a user?
        val result = call.muteUser("tommaso")
        assertSuccess(result)
        // TODO: check event?
    }

    @Test
    fun `Edtech style moderation`() = runTest {

        // a teacher mutes the audio for a student

        // a teacher mutes everything for a student

        // this needs to fire an event that update's Tommaso's permissions...

        // the student is not able to unmute themselves
        // so state for the microphone is on/off/not allowed
        call.microphone.status // on/off/no call permission/ no android permission

        // they can request to unmute though
        call.requestPermissions("unmute")

        // the teacher grants this request

        // again, an event should update permissions

        // and the student can click unmute
        // same applies for audio, video and screen sharing
    }

    @Test
    fun `Zoom style moderation`() = runTest {
        // on a large call someone will keep background noise going
        // you want to mute everyone at once

        // people can unmute themselves
    }
}
