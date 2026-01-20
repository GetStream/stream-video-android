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

package io.getstream.video.android.core.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class AtomicUnitCallTest {

    @Test
    fun `invoke should execute the block`() {
        val atomicUnitCall = AtomicUnitCall()
        var executed = false
        atomicUnitCall {
            executed = true
        }
        assertTrue(executed)
    }

    @Test
    fun `invoke should execute the block only once`() {
        val atomicUnitCall = AtomicUnitCall()
        var executed = 0
        atomicUnitCall {
            executed++
        }
        atomicUnitCall {
            executed++
        }
        assert(executed == 1)
    }
}
