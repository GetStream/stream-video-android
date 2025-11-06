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

package io.getstream.video.android.core.call.connection.job

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RestartIceJobDelegateTest {
    private lateinit var testScope: TestScope
    private lateinit var delegate: RestartIceJobDelegate

    @Before
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher())
        delegate = RestartIceJobDelegate(testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `should invoke restartIce after delay`() = testScope.runTest {
        var called = false

        delegate.scheduleRestartIce(100) {
            called = true
        }

        // Before delay: should not yet be called
        advanceTimeBy(99)
        assertFalse(called, "restartIce() should not be called before timeout")

        // After delay: should have called
        advanceTimeBy(2)
        assertTrue(called, "restartIce() should be called after delay")
    }

    @Test
    fun `should cancel previous job when scheduling new restart`() = testScope.runTest {
        var firstCalled = false
        var secondCalled = false

        delegate.scheduleRestartIce(100) {
            firstCalled = true
        }

        // Schedule a second one before first executes
        advanceTimeBy(50)
        delegate.scheduleRestartIce(100) {
            secondCalled = true
        }

        // Advance time enough for both timeouts to elapse
        advanceTimeBy(200)

        assertFalse(firstCalled, "First scheduled job should have been cancelled")
        assertTrue(secondCalled, "Second scheduled job should have executed")
    }

    @Test
    fun `should cancel scheduled job when cancelScheduledRestartIce is called`() = testScope.runTest {
        var called = false

        delegate.scheduleRestartIce(100) {
            called = true
        }

        // Cancel before timeout
        delegate.cancelScheduledRestartIce()

        advanceTimeBy(200)
        assertFalse(called, "Job should be cancelled and restartIce() not called")
    }
}
