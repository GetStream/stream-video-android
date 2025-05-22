/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.subscriptions.api

import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.subscriptions.impl.DefaultDebouncedSubscriptionManager
import io.getstream.video.android.core.subscriptions.impl.DefaultManualOverridesSubscriptionManager
import io.getstream.video.android.core.subscriptions.impl.DefaultSubscriptionManager
import io.getstream.video.android.core.subscriptions.impl.DefaultViewPortBasedSubscriptionManager
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FactoriesTest {
    private lateinit var scope: CoroutineScope
    private lateinit var signalingService: SignalServerService
    private val sessionId = "test-session"

    @Before
    fun setup() {
        scope = TestScope()
        signalingService = mockk(relaxed = true)
    }

    @Test
    fun `subscriptionManager returns DefaultSubscriptionManager`() {
        val manager = subscriptionManager(sessionId, signalingService)
        assertTrue(manager is DefaultSubscriptionManager)
    }

    @Test
    fun `debouncedSubscriptionManager returns DefaultDebouncedSubscriptionManager`() {
        val base = subscriptionManager(sessionId, signalingService)
        val manager = debouncedSubscriptionManager(scope, base)
        assertTrue(manager is DefaultDebouncedSubscriptionManager)
    }

    @Test
    fun `viewportBasedSubscriptionManager returns DefaultViewPortBasedSubscriptionManager`() {
        val base = subscriptionManager(sessionId, signalingService)
        val debounced = debouncedSubscriptionManager(scope, base)
        val manager = viewportBasedSubscriptionManager(debounced)
        assertTrue(manager is DefaultViewPortBasedSubscriptionManager)
    }

    @Test
    fun `manualOverridesSubscriptionManager returns DefaultManualOverridesSubscriptionManager`() {
        val base = subscriptionManager(sessionId, signalingService)
        val debounced = debouncedSubscriptionManager(scope, base)
        val viewport = viewportBasedSubscriptionManager(debounced)
        val manager = manualOverridesSubscriptionManager(viewport)
        assertTrue(manager is DefaultManualOverridesSubscriptionManager)
    }

    @Test
    fun `defaultStreamSubscriptionManager returns DefaultManualOverridesSubscriptionManager`() {
        val manager = defaultStreamSubscriptionManager(scope, sessionId, signalingService)
        assertTrue(manager is DefaultManualOverridesSubscriptionManager)
    }
}
