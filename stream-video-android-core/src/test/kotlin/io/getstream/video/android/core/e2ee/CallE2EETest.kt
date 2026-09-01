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

package io.getstream.video.android.core.e2ee

import com.google.common.truth.Truth.assertThat
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.injectSession
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.model.User
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender

/**
 * Covers the [Call]-level end-to-end encryption surface: attaching a manager, the guard that keeps
 * it before join, the key conveniences, and the flag the coordinator validates the join against.
 */
class CallE2EETest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    private val testScope = TestScope(StandardTestDispatcher())

    @RelaxedMockK
    private lateinit var mockClientImpl: StreamVideoClient

    private lateinit var call: Call

    /** Encrypts nothing; the tests only care about what the SDK asks of a manager. */
    private open class NoopE2EEManager : E2EEManager {
        override fun encrypt(sender: RtpSender, codec: String?, trackType: E2EETrackType?) = Unit
        override fun decrypt(receiver: RtpReceiver, userId: String, trackType: E2EETrackType?) =
            Unit
    }

    private class KeyedE2EEManager : NoopE2EEManager(), E2EEKeyProvider {
        val sharedKeys = mutableMapOf<Int, ByteArray>()
        val userKeys = mutableMapOf<Pair<String, Int>, ByteArray>()
        var removedAll = false

        override fun setSharedKey(keyIndex: Int, key: ByteArray) {
            sharedKeys[keyIndex] = key
        }

        override fun setKey(userId: String, keyIndex: Int, key: ByteArray) {
            userKeys[userId to keyIndex] = key
        }

        override fun removeSharedKey(keyIndex: Int) {
            sharedKeys.remove(keyIndex)
        }

        override fun removeKey(userId: String, keyIndex: Int) {
            userKeys.remove(userId to keyIndex)
        }

        override fun removeAllKeys() {
            removedAll = true
        }
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        StreamVideo.install(mockk(relaxed = true))

        val networkStateProvider = mockk<NetworkStateProvider>(relaxed = true) {
            every { isConnected() } returns true
        }
        every { mockClientImpl.coordinatorConnectionModule } returns
            mockk<CoordinatorConnectionModule>(relaxed = true) {
                every { this@mockk.networkStateProvider } returns networkStateProvider
            }
        every { mockClientImpl.scope } returns testScope as CoroutineScope
        every { mockClientImpl.apiKey } returns "test-api-key"
        // Keeps joinRequest off the success path, which would try to apply a mocked response.
        coEvery {
            mockClientImpl.joinCall(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.Failure(Error.GenericError("not under test"))

        call = Call(
            client = mockClientImpl,
            type = "default",
            id = "test-call",
            user = User(id = "test-user", role = "user"),
        )
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    @Test
    fun `attaching a manager marks the call as encrypted`() {
        assertThat(call.state.e2eeEnabled.value).isFalse()

        call.setE2EEManager(NoopE2EEManager())

        assertThat(call.state.e2eeEnabled.value).isTrue()
    }

    @Test
    fun `detaching the manager clears the encrypted state`() {
        call.setE2EEManager(NoopE2EEManager())

        call.setE2EEManager(null)

        assertThat(call.state.e2eeEnabled.value).isFalse()
    }

    @Test
    fun `attaching a manager after join is rejected`() {
        // The publisher and subscriber capture the manager when the session is built, so a manager
        // attached afterwards would silently apply to nothing.
        call.injectSession(mockk<RtcSession>(relaxed = true))

        val failure = assertThrows(IllegalStateException::class.java) {
            call.setE2EEManager(NoopE2EEManager())
        }

        assertThat(failure).hasMessageThat().contains("before join()")
        assertThat(call.state.e2eeEnabled.value).isFalse()
    }

    @Test
    fun `key helpers forward to a manager that provides keys`() {
        val manager = KeyedE2EEManager()
        call.setE2EEManager(manager)
        val key = ByteArray(16) { it.toByte() }

        call.setE2EESharedKey(keyIndex = 1, key = key)
        call.setE2EEKey(userId = "alice", keyIndex = 2, key = key)

        assertThat(manager.sharedKeys[1]).isEqualTo(key)
        assertThat(manager.userKeys["alice" to 2]).isEqualTo(key)

        call.removeE2EESharedKey(keyIndex = 1)
        call.removeE2EEKey(userId = "alice", keyIndex = 2)
        call.removeAllE2EEKeys()

        assertThat(manager.sharedKeys).isEmpty()
        assertThat(manager.userKeys).isEmpty()
        assertThat(manager.removedAll).isTrue()
    }

    @Test
    fun `key helpers are rejected for a manager that has no key provider`() {
        call.setE2EEManager(NoopE2EEManager())

        val failure = assertThrows(IllegalStateException::class.java) {
            call.setE2EESharedKey(keyIndex = 0, key = ByteArray(16))
        }

        assertThat(failure).hasMessageThat().contains("E2EEKeyProvider")
    }

    @Test
    fun `the join request reports e2ee when a manager is attached`() = runTest {
        call.setE2EEManager(NoopE2EEManager())

        call.joinRequest(
            location = "test-location",
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )

        coVerify {
            mockClientImpl.joinCall(
                type = any(),
                id = any(),
                create = any(),
                members = any(),
                custom = any(),
                settingsOverride = any(),
                startsAt = any(),
                team = any(),
                ring = any(),
                notify = any(),
                location = any(),
                migratingFrom = any(),
                migratingFromList = any(),
                hintHighScaleLivestreamPublisher = any(),
                e2ee = true,
            )
        }
    }

    @Test
    fun `the join request reports no e2ee when no manager is attached`() = runTest {
        call.joinRequest(
            location = "test-location",
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )

        coVerify {
            mockClientImpl.joinCall(
                type = any(),
                id = any(),
                create = any(),
                members = any(),
                custom = any(),
                settingsOverride = any(),
                startsAt = any(),
                team = any(),
                ring = any(),
                notify = any(),
                location = any(),
                migratingFrom = any(),
                migratingFromList = any(),
                hintHighScaleLivestreamPublisher = any(),
                e2ee = false,
            )
        }
    }
}
