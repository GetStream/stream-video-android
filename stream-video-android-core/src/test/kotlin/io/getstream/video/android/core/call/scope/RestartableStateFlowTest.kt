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

package io.getstream.video.android.core.call.scope

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RestartableStateFlowTest {
    @Test
    fun `provider restart moves production without replacing collectors`() = runTest {
        val scopeProvider = ScopeProviderImpl(backgroundScope, "default:test-call")
        val upstream = MutableStateFlow(1)
        val state = RestartableStateFlow(
            upstream = upstream,
            scopeProvider = scopeProvider,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
        )
        val values = mutableListOf<Int>()
        val collector = backgroundScope.launch { state.collect(values::add) }
        runCurrent()
        assertEquals(1, state.value)

        scopeProvider.cleanup()?.join()
        upstream.value = 2
        runCurrent()
        assertEquals(1, state.value)

        scopeProvider.restart()
        runCurrent()

        assertEquals(2, state.value)
        assertFalse(collector.isCancelled)
        assertEquals(listOf(0, 1, 0, 2), values)
    }

    @Test
    fun `while subscribed starts and stops upstream using actual subscriber count`() = runTest {
        val scope = ScopeProviderImpl(backgroundScope, "default:test-call")
        var active = 0
        val state = flow {
            active++
            try {
                emit(1)
                awaitCancellation()
            } finally { active-- }
        }.restartableStateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), 0)
        runCurrent()
        assertEquals(0, active)
        val collector = backgroundScope.launch { state.collect {} }
        runCurrent()
        assertEquals(1, active)
        collector.cancelAndJoin()
        runCurrent()
        assertEquals(0, active)
    }

    @Test
    fun `detached launches cannot run without an owner`() = runTest {
        val scope = ScopeProviderImpl(backgroundScope, "default:test-call")
        scope.cleanup()?.join()
        var ran = false
        val job = scope.callScope.launch { ran = true }
        job.join()
        assertFalse(ran)
    }

    @Test
    fun `replay expiration restores initial value`() = runTest {
        val scope = ScopeProviderImpl(backgroundScope, "default:test-call")
        val upstream = MutableStateFlow(1)
        val state = upstream.restartableStateIn(
            scope,
            SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            0,
        )
        val collector = backgroundScope.launch { state.collect {} }
        runCurrent()
        assertEquals(1, state.value)
        collector.cancelAndJoin()
        runCurrent()
        assertEquals(0, state.value)
    }
}
