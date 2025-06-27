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

package io.getstream.video.android.core.utils

import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TaskSchedulerWithDebounceTest {
    private lateinit var scheduler: TaskSchedulerWithDebounce
    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        scheduler = TaskSchedulerWithDebounce()
        testScheduler = TestCoroutineScheduler()
        testDispatcher = StandardTestDispatcher(testScheduler)
        testScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `task is executed immediately if debounce is zero`() = runTest {
        var executed = false
        scheduler.schedule(this, ScheduleConfig(debounce = { 0 })) {
            executed = true
        }
        assertEquals(true, executed)
    }

    @Test
    fun `task is executed after debounce delay`() = runTest {
        var executed = false
        scheduler.schedule(testScope, ScheduleConfig(debounce = { 100 })) {
            executed = true
        }
        assertEquals(false, executed)
        testScheduler.advanceTimeBy(100)
        testScope.runCurrent()
        assertEquals(true, executed)
    }

    @Test
    fun `previous task is cancelled if rescheduled before debounce`() = runTest {
        val counter = AtomicInteger(0)
        scheduler.schedule(testScope, ScheduleConfig(debounce = { 100 })) {
            counter.incrementAndGet()
        }
        // Reschedule before debounce time
        testScheduler.advanceTimeBy(50)
        scheduler.schedule(testScope, ScheduleConfig(debounce = { 100 })) {
            counter.incrementAndGet()
        }
        testScheduler.advanceTimeBy(100)
        testScope.runCurrent()
        assertEquals(1, counter.get())
    }

    @Test
    fun `multiple rapid schedules only execute last`() = runTest {
        val counter = AtomicInteger(0)
        repeat(5) {
            scheduler.schedule(testScope, ScheduleConfig(debounce = { 100 })) {
                counter.incrementAndGet()
            }
            testScheduler.advanceTimeBy(20)
        }
        // Only the last one should execute after 100ms from last schedule
        testScheduler.advanceTimeBy(100)
        testScope.runCurrent()
        assertEquals(1, counter.get())
    }
}
