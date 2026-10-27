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

import io.getstream.result.Error
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallCleanupStateTest {

    @Test
    fun `joining an active call does not restart its lifecycle`() = runTest {
        val cleanupState = CallCleanupState()
        var restartInvoked = false

        val result = cleanupState.prepareForJoin {
            restartInvoked = true
            Success(Unit)
        }

        assertEquals(Success(Unit), result)
        assertFalse(restartInvoked)
        assertFalse(cleanupState.isDestroyed)
    }

    @Test
    fun `failed restart keeps the call destroyed so a later join can retry`() = runTest {
        val cleanupState = CallCleanupState()
        cleanupState.startLeave {
            Job().apply { complete() }
        }
        cleanupState.cleanupOnce {}
        val failure = Failure(Error.GenericError("scope restart failed"))

        assertEquals(failure, cleanupState.prepareForJoin { failure })
        assertTrue(cleanupState.isDestroyed)

        assertEquals(Success(Unit), cleanupState.prepareForJoin { Success(Unit) })
        assertFalse(cleanupState.isDestroyed)
    }

    @Test
    fun `leave and cleanup after restart destroy the new lifecycle`() = runTest {
        val cleanupState = CallCleanupState()
        cleanupState.cleanupOnce {}
        assertEquals(Success(Unit), cleanupState.prepareForJoin { Success(Unit) })
        assertFalse(cleanupState.isDestroyed)

        var cleanupInvoked = false
        cleanupState.startLeave {
            Job().apply { complete() }
        }
        cleanupState.cleanupOnce { cleanupInvoked = true }

        assertTrue(cleanupInvoked)
        assertTrue(cleanupState.isDestroyed)
    }
}
