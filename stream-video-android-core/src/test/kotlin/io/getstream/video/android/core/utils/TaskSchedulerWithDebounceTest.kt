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

import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSchedulerWithDebounceTest {
    private var scheduler: TaskSchedulerWithDebounce = TaskSchedulerWithDebounce()

    @Test
    fun `task is executed immediately if debounce is zero`() = runTest {
        var executed = false
        scheduler.schedule(this.backgroundScope, ScheduleConfig(debounce = { 0 })) {
            executed = true
        }
        assertEquals(true, executed)
    }
}
