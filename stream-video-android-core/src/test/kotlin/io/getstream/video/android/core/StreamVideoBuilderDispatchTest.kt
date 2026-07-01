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

package io.getstream.video.android.core

import android.content.Context
import android.net.ConnectivityManager
import io.getstream.android.core.api.StreamClient
import io.getstream.video.android.core.socket.coordinator.v2.GuestStreamTokenProvider
import io.getstream.video.android.core.socket.coordinator.v2.IntegrationStreamTokenProvider
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Dispatch-level tests for [StreamVideoBuilder] focused on the token-provider selection wired in
 * Plan 02-02 (D-05, D-06, D-07) and the `localCoordinatorAddress` -> `ws://` plumbing (COORD-08).
 *
 * Uses the [StreamVideoBuilder.streamClientFactory] seam to stub the [StreamClient] factory so
 * the tests never call core's Android-service-dependent initialisation path (RESEARCH Pitfall 2).
 */
internal class StreamVideoBuilderDispatchTest {

    @Test
    fun `Anonymous user throws IllegalStateException with D-07 reference`() {
        val builder = builderFor(
            user = User(id = "anon-1", type = UserType.Anonymous),
        )

        val err = assertFailsWith<IllegalStateException> { builder.build() }
        assertTrue(
            err.message?.contains("D-07") == true,
            "Expected IllegalStateException to reference D-07, got: ${err.message}",
        )
    }

    @Test
    fun `Authenticated path selects IntegrationStreamTokenProvider`() {
        val capturedProvider = captureFactoryArgsFor(
            user = User(id = "auth-1", type = UserType.Authenticated),
            token = "user-token",
        )?.tokenProvider
        assertTrue(
            capturedProvider is IntegrationStreamTokenProvider,
            "Expected IntegrationStreamTokenProvider, got: ${capturedProvider?.javaClass}",
        )
    }

    @Test
    fun `Guest path selects GuestStreamTokenProvider`() {
        val capturedProvider = captureFactoryArgsFor(
            user = User(id = "guest-1", type = UserType.Guest),
            token = "",
        )?.tokenProvider
        assertTrue(
            capturedProvider is GuestStreamTokenProvider,
            "Expected GuestStreamTokenProvider, got: ${capturedProvider?.javaClass}",
        )
    }

    @Test
    fun `localCoordinatorAddress threads into resolvedWssUrl as ws`() {
        val user = User(id = "auth-1", type = UserType.Authenticated)
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns mockk<ConnectivityManager>(relaxed = true)
        val builder = StreamVideoBuilder(
            context = context,
            apiKey = "apikey",
            user = user,
            token = "user-token",
            localCoordinatorAddress = "10.0.2.2:3030",
        )
        var capturedUrl: String? = null
        builder.streamClientFactory = { args ->
            capturedUrl = args.resolvedWssUrl
            mockk<StreamClient>(relaxed = true)
        }
        runCatching { builder.build() }
        assertEquals("ws://10.0.2.2:3030/video/connect", capturedUrl)
    }

    private fun builderFor(user: User, token: String = "token"): StreamVideoBuilder {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns mockk<ConnectivityManager>(relaxed = true)
        return StreamVideoBuilder(
            context = context,
            apiKey = "apikey",
            user = user,
            token = token,
        )
    }

    /**
     * Substitutes the `streamClientFactory` seam so the factory args can be observed without
     * invoking core's real factory.
     */
    private fun captureFactoryArgsFor(user: User, token: String): StreamClientFactoryArgs? {
        val builder = builderFor(user = user, token = token)
        var captured: StreamClientFactoryArgs? = null
        builder.streamClientFactory = { args ->
            captured = args
            mockk<StreamClient>(relaxed = true)
        }
        runCatching { builder.build() }
        return captured
    }
}
