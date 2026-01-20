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

package io.getstream.video.android.core.stories

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.BlockedUserEvent
import io.getstream.android.video.generated.models.CallMemberRemovedEvent
import io.getstream.android.video.generated.models.CallMemberUpdatedPermissionEvent
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.PermissionRequestEvent
import io.getstream.video.android.core.base.IntegrationTestBase
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 *   https://www.notion.so/stream-wiki/Moderation-Permissions-for-video-37a3376268654095b9aafaba12d4bb69
 *   https://www.notion.so/stream-wiki/Call-Permissions-832f914ad4c545cf8f048012900ad21d
 */
@RunWith(RobolectricTestRunner::class)
class ModerationTest : IntegrationTestBase() {

    // TODO: Ignored since the backend doesn't seem to fire the event
    @Test
    @Ignore
    fun `Basic moderation - Block a user`() = runTest {
        val response = call.blockUser("tommaso")
        assertSuccess(response)
        val event = waitForNextEvent<BlockedUserEvent>()
        assertThat(event.user.id).isEqualTo("tommaso")
    }

    // TODO: Ignored since the backend doesn't seem to fire the event
    @Test
    @Ignore
    fun `Basic moderation - Remove a member`() = runTest {
        // create a call with Thierry & Tommaso
        val members = mutableListOf("thierry", "tommaso")
        val call = client.call("default", randomUUID())
        val createResult = call.create(memberIds = members, custom = mapOf("color" to "red"))
        assertSuccess(createResult)
        assertThat(call.state.members.value.map { it.user.id }).contains("tommaso")
        // now remove Tommaso
        val response = call.removeMembers(listOf("tommaso"))
        assertSuccess(response)

        // Verify call.get no longer returns Tommaso
        val callResult = call.get()
        assertSuccess(callResult)
        callResult.onSuccess {
            assertThat(it.members.map { it.user.id }).doesNotContain("tommaso")
        }
        // TODO: verify we receive the event (this doesn't work)
        val event = waitForNextEvent<CallMemberRemovedEvent>()
        assertThat(event.members).contains("tommaso")
        // verify state is updated
        assertThat(call.state.members.value.map { it.user.id }).doesNotContain("tommaso")
    }

    // TODO: Ignored since the backend doesn't seem to fire the event
    @Test
    @Ignore
    fun `Basic moderation - Request permission to share your screen`() = runTest {
        val hasPermission = call.state.hasPermission("screenshare").value
        assertThat(hasPermission).isTrue()

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
        val result = call.muteUser("tommaso")
        assertSuccess(result)
    }

    @Test
    @Ignore
    fun `Basic moderation - Request permission to talk`() = runTest {
        Thread.sleep(100L)
        client.subscribe {
            println("event $it all events ${events.map { it.javaClass.simpleName }}")
        }
        val ownCapabilities = call.state.ownCapabilities.value
        // TODO: maybe not use strings for the permissions
        assertThat(ownCapabilities).contains(OwnCapability.SendAudio)
        val hasPermission = call.state.hasPermission("send-audio").value
        assertThat(hasPermission).isTrue()

        Thread.sleep(100L)

        val response = call.requestPermissions("send-audio")
        Thread.sleep(100L)
        assertSuccess(response)
        waitForNextEvent<PermissionRequestEvent>().also {
            assertThat(it.user.id).isEqualTo(client.user.id)
            assertThat(it.permissions).contains("send-audio")
        }
        Thread.sleep(100L)

        val permissionRequest = call.state.permissionRequests.value.first()
        val grantResult = permissionRequest.grant()
        assertSuccess(grantResult)
    }
}
