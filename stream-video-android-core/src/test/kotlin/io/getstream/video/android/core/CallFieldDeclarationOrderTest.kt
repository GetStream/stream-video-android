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

package io.getstream.video.android.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Structural regression guard for a leaking-this race in [Call]'s primary constructor.
 *
 * `Call` declares `val state = CallState(client, this, user, scope)` which passes
 * `this` to [CallState]. CallState constructs [io.getstream.video.android.core.sorting.SortedParticipantsState],
 * whose `init` launches `scope.launch { call.events.collect { ... } }`. The
 * `events` flow is owned by `eventManager` (CallEventManager) and `Call.events`
 * is assigned from it. Kotlin runs field initializers in textual order, so if
 * `eventManager` is declared after `state`, the launched coroutine reads
 * `call.events` while the backing field is still null and NPEs into the scope's
 * CoroutineExceptionHandler on production dispatchers (Default/Main/IO).
 *
 * The full test suite uses [kotlinx.coroutines.test.UnconfinedTestDispatcher],
 * which captures the NPE silently in the scope's exception handler — no
 * existing test fails. This structural check is the cheapest, least-flaky
 * guard against a future refactor reordering the declarations and
 * re-introducing the race.
 *
 * Reads [Call] from the module's source directory because the Gradle test
 * working directory is the module root.
 */
class CallFieldDeclarationOrderTest {

    @Test
    fun `Call_events is declared before Call_state to prevent NPE in sorted-participants init`() {
        val callSource = File(
            "src/main/kotlin/io/getstream/video/android/core/Call.kt",
        ).readText()

        val lines = callSource.lines()
        val eventManagerLine = lines.indexOfFirst {
            it.contains("val eventManager = CallEventManager(")
        }
        val eventsLine = lines.indexOfFirst {
            it.contains("val events: MutableSharedFlow<VideoEvent> = eventManager.events")
        }
        val stateLine = lines.indexOfFirst {
            it.contains("val state = CallState(")
        }

        assertThat(eventManagerLine).isGreaterThan(-1)
        assertThat(eventsLine).isGreaterThan(-1)
        assertThat(stateLine).isGreaterThan(-1)
        assertThat(eventManagerLine).isLessThan(stateLine)
        assertThat(eventsLine).isLessThan(stateLine)
    }
}
