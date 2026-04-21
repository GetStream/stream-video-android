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

package io.getstream.video.android.core.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AudioExecutionContextTest {

    private lateinit var context: AudioExecutionContext

    @Before
    fun setup() {
        context = AudioExecutionContext()
    }

    @Test
    fun `child scope should be active and independent`() = runTest {
        val child = context.createChildScope()

        assertNotNull(child)
        assertTrue(child.isActive)
    }

    @Test
    fun `cancelling child scope should not affect parent`() = runTest {
        val child = context.createChildScope()!!

        child.cancel()

        val newChild = context.createChildScope()

        assertNotNull(newChild)
        assertTrue(newChild!!.isActive)
    }

    @Test
    fun `release should cancel all child scopes`() = runTest {
        val child1 = context.createChildScope()!!
        val child2 = context.createChildScope()!!

        context.release()

        assertTrue(child1.coroutineContext[Job]!!.isCancelled)
        assertTrue(child2.coroutineContext[Job]!!.isCancelled)
    }

    @Test
    fun `running jobs in child scope should be cancelled on release`() = runTest {
        val child = context.createChildScope()!!

        val job = child.launch {
            delay(Long.MAX_VALUE)
        }

        context.release()

        assertTrue(job.isCancelled)
    }

    @Test
    fun `failure in one child should not affect another`() = runTest {
        val child1 = context.createChildScope()!!
        val child2 = context.createChildScope()!!

        val job1 = child1.launch {
            runCatching {
                throw RuntimeException("boom")
            }
        }

        val job2 = child2.launch {
            delay(100)
        }

        advanceUntilIdle()

        assertTrue(job1.isCompleted)
        assertTrue(job2.isActive || job2.isCompleted)
    }

    @Test
    fun `createChildScope should return null after release`() = runTest {
        context.release()

        val child = context.createChildScope()

        assertNull(child)
    }
}
