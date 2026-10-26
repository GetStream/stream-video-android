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

package io.getstream.video.android.core.call.components

import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.Restartable
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.scope.ScopeProviderImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallLifecycleManagerTest {
    private class Fixture(test: TestScope) {
        val session = mockk<RtcSession>(relaxed = true)
        val sessions = CallSessionManager().apply { setActiveSession(this@Fixture.session) }
        val state = mockk<CallState>(relaxed = true)
        var restarts = 0
        var shutdowns = 0
        private val parent = test.backgroundScope
        val scopeProvider = ScopeProviderImpl(parent, "default:test-call")
        val scope: CoroutineScope get() = scopeProvider.callScope
        init {
            every { state._connection } returns MutableStateFlow(RealtimeConnection.PreJoin)
        }
        private val client = mockk<StreamVideoClient>(relaxed = true).also {
            every { it.scope } returns parent
        }
        val lifecycle = CallLifecycleManager(
            clientImpl = client,
            sessionManager = sessions,
            callRegistry = mockk(relaxed = true),
            shutDownJobs = {
                shutdowns++
                scopeProvider.cleanup()
            },
            restartCoordinator = CallRestartCoordinator(
                scopeProvider = Restartable {
                    restarts++
                    scopeProvider.restart()
                },
                restartables = emptyList(),
            ),
            state = state,
            callAnalytics = mockk(relaxed = true),
            statsReporter = mockk(relaxed = true),
            media = mockk(relaxed = true),
            iceMonitor = mockk(relaxed = true),
            sessionMonitor = { mockk(relaxed = true) },
            connectivityMonitor = { mockk(relaxed = true) },
            type = "default",
            id = "test-call",
        )
    }

    @Test
    fun `immediate concurrent rejoins wait for cleanup and restart only once`() = runTest {
        val fixture = Fixture(this)
        val leaveEvent = CompletableDeferred<Unit>()
        coEvery { fixture.session.sendLeaveEvent(any()) } coAnswers { leaveEvent.await() }
        fixture.lifecycle.leave()
        fixture.lifecycle.leave()
        val first = async { fixture.lifecycle.prepareForJoin() }
        val second = async { fixture.lifecycle.prepareForJoin() }
        runCurrent()
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        assertEquals(0, fixture.restarts)
        leaveEvent.complete(Unit)
        first.await()
        second.await()
        assertEquals(1, fixture.shutdowns)
        assertEquals(1, fixture.restarts)
        assertFalse(fixture.lifecycle.isDestroyed)
    }

    @Test
    fun `rejoin waits for the cancelled call producers to finish their finalizers`() = runTest {
        val fixture = Fixture(this)
        val finished = CompletableDeferred<Unit>()
        fixture.scope.launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { finished.await() }
            }
        }
        runCurrent()
        fixture.lifecycle.leave()
        val rejoin = async { fixture.lifecycle.prepareForJoin() }
        runCurrent()
        assertFalse(rejoin.isCompleted)
        assertEquals(0, fixture.restarts)
        finished.complete(Unit)
        rejoin.await()
        assertEquals(1, fixture.restarts)
    }

    @Test
    fun `repeated leave join cycles clean up each generation`() = runTest {
        val fixture = Fixture(this)
        repeat(3) { cycle ->
            fixture.sessions.setActiveSession(fixture.session)
            fixture.lifecycle.leave()
            fixture.lifecycle.prepareForJoin()
            assertEquals(cycle + 1, fixture.shutdowns)
            assertEquals(cycle + 1, fixture.restarts)
            assertTrue(fixture.scope.coroutineContext[Job]!!.isActive)
        }
    }

    @Test
    fun `leave without another join releases resources without restarting producers`() = runTest {
        val fixture = Fixture(this)
        fixture.lifecycle.leave()
        runCurrent()
        assertEquals(1, fixture.shutdowns)
        assertEquals(0, fixture.restarts)
        assertTrue(fixture.scope.coroutineContext[Job]!!.isCancelled)
        verify { fixture.state.cleanup() }
    }
}
