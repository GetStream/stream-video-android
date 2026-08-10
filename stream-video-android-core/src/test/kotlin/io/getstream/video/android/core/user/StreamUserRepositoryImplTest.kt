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

package io.getstream.video.android.core.user

import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class StreamUserRepositoryImplTest {

    private val initial = User(id = "initial", type = UserType.Authenticated)
    private val replacement = User(id = "server_issued", type = UserType.Guest)

    @Test
    fun `user returns the seed passed to the constructor`() {
        val repo = StreamUserRepositoryImpl(initial)

        assertSame(initial, repo.user)
    }

    @Test
    fun `userFlow holds the seed as its initial value`() = runTest {
        val repo = StreamUserRepositoryImpl(initial)

        assertSame(initial, repo.userFlow.value)
        assertSame(initial, repo.userFlow.first())
    }

    @Test
    fun `setUser updates user and userFlow_value atomically`() {
        val repo = StreamUserRepositoryImpl(initial)

        repo.setUser(replacement)

        assertSame(replacement, repo.user)
        assertSame(replacement, repo.userFlow.value)
    }

    // Validates that `state.user` (which is the same StateFlow under the hood) sees
    // the adopted identity instead of being stuck on the integrator-supplied seed.
    @Test
    fun `setUser emits the new value to active collectors`() = runTest {
        val repo = StreamUserRepositoryImpl(initial)

        val collected = mutableListOf<User>()
        // UNDISPATCHED so the collector subscribes before setUser fires — otherwise
        // StateFlow's conflation would drop the initial value and take(2) would hang.
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repo.userFlow.take(2).toList(collected)
        }

        repo.setUser(replacement)
        job.join()

        assertEquals(listOf(initial, replacement), collected)
    }

    // StateFlow is conflated by reference equality — re-setting the same instance
    // shouldn't re-emit, but a distinct instance with the same data should still flow
    // through. Guards against accidentally treating equality semantics as identity.
    @Test
    fun `setUser with a structurally different user replaces the value`() {
        val repo = StreamUserRepositoryImpl(initial)
        val sameIdDifferentInstance = User(id = "initial", type = UserType.Anonymous)

        repo.setUser(sameIdDifferentInstance)

        assertSame(sameIdDifferentInstance, repo.user)
        assertNotSame(initial, repo.user)
    }
}
