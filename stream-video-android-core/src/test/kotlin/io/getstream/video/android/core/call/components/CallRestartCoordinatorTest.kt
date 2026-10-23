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

import io.getstream.video.android.core.call.Restartable
import org.junit.Test
import kotlin.test.assertEquals

class CallRestartCoordinatorTest {

    @Test
    fun `restart recreates scope before restarting components and completing bookkeeping`() {
        val events = mutableListOf<String>()
        val coordinator = CallRestartCoordinator(
            scopeProvider = Restartable { events += "scope" },
            restartables = listOf(
                Restartable { events += "state" },
                Restartable { events += "media" },
            ),
            onRestarted = { events += "bookkeeping" },
        )

        coordinator.restart()

        assertEquals(listOf("scope", "state", "media", "bookkeeping"), events)
    }
}
