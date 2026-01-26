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

package io.getstream.video.android.core.base

import io.getstream.video.android.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

public class DispatcherRule(
    public val testDispatcher: TestDispatcher = sharedTestDispatcher,
) : TestWatcher() {
    override fun starting(description: Description) {
        println("setting up test dispatcher $testDispatcher")
        Dispatchers.setMain(testDispatcher)
        DispatcherProvider.set(testDispatcher, testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
        DispatcherProvider.reset()
    }

    companion object {
        // The correct way is to not reuse the test dispatcher between tests, but in our
        // case we have a shared/cached StreamVideoImpl in IntegrationTestBase which
        // has a reference to the dispatcher - so we need to keep using it. In future we
        // need to refactor this to recreate the SDK instance for each test (or only reuse it
        // for specific integration tests).
        private val sharedTestDispatcher: TestDispatcher =
            UnconfinedTestDispatcher(TestCoroutineScheduler())
    }
}
