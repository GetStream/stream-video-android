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

package io.getstream.video.android.core.socket.coordinator.v2

import io.getstream.android.core.api.model.value.StreamUserId
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.CreateGuestRequest
import io.getstream.android.video.generated.models.CreateGuestResponse
import io.getstream.android.video.generated.models.UserResponse
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import kotlin.test.assertEquals

internal class GuestStreamTokenProviderTest {

    @Test
    fun `loadToken calls createGuest once and adopts server-issued user`() = runTest {
        val api = mockk<ProductvideoApi>()
        val repo = mockk<WritableUserRepository>(relaxed = true)
        val initialUser = User(
            id = "guest-local",
            image = "https://example.com/avatar.png",
            name = "Guest Local",
            custom = mapOf("k" to "v"),
            type = UserType.Guest,
        )
        coEvery { api.createGuest(any()) } returns CreateGuestResponse(
            accessToken = "fake-jwt",
            duration = "10ms",
            user = serverIssuedUser(),
        )
        val subject = GuestStreamTokenProvider(api, initialUser, repo)

        val token = subject.loadToken(StreamUserId.fromString("guest-1"))

        coVerify(exactly = 1) { api.createGuest(any()) }
        assertEquals("fake-jwt", token.rawValue)
        verify(exactly = 1) {
            repo.setUser(
                match { user ->
                    user.id == "server-issued-id" && user.type == UserType.Guest
                },
            )
        }
    }

    @Test
    fun `UserRequest carries initial user fields`() = runTest {
        val api = mockk<ProductvideoApi>()
        val repo = mockk<WritableUserRepository>(relaxed = true)
        val initialUser = User(
            id = "guest-local",
            image = "https://example.com/pic.png",
            name = "Local Name",
            custom = mapOf("locale" to "en_US"),
            type = UserType.Guest,
        )
        val captured = slot<CreateGuestRequest>()
        coEvery { api.createGuest(capture(captured)) } returns CreateGuestResponse(
            accessToken = "fake-jwt",
            duration = "5ms",
            user = serverIssuedUser(),
        )
        val subject = GuestStreamTokenProvider(api, initialUser, repo)

        subject.loadToken(StreamUserId.fromString("guest-1"))

        assertEquals("guest-local", captured.captured.user.id)
        assertEquals("https://example.com/pic.png", captured.captured.user.image)
        assertEquals("Local Name", captured.captured.user.name)
        assertEquals("en_US", captured.captured.user.custom?.get("locale"))
    }

    private fun serverIssuedUser(): UserResponse = UserResponse(
        createdAt = FIXED_TIME,
        id = "server-issued-id",
        language = "en",
        role = "user",
        updatedAt = FIXED_TIME,
    )

    private companion object {
        val FIXED_TIME: OffsetDateTime =
            OffsetDateTime.parse("2026-01-01T00:00:00Z")
    }
}
