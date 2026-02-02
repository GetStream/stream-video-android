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

package io.getstream.video.android.core.coroutines.flows

import io.getstream.video.android.core.coroutines.scopes.RestartableProducerScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A [StateFlow] implementation whose upstream collection can be safely restarted
 * when the underlying call coroutine scope is cancelled and recreated.
 *
 * ## Why this exists
 * The standard `stateIn(scope, ...)` operator permanently binds upstream collection
 * to a single [CoroutineScope]. When that scope is cancelled (for example after
 * `call.leave()`), the StateFlow stops updating forever and cannot be restarted.
 *
 * In this SDK, call- and participant-level state must survive leave/join cycles,
 * while the call coroutine scope is intentionally cancelled and recreated.
 *
 * ## How this replaces `stateIn`
 * Instead of tying upstream collection to a fixed scope, [RestartableStateFlow]
 * separates:
 * - **State storage** (a stable [StateFlow] exposed to clients)
 * - **Producer lifecycle** (a coroutine collecting the upstream Flow)
 *
 * The producer coroutine is started and restarted via [RestartableProducerScope]
 * whenever a new call scope is attached, while the StateFlow instance itself
 * remains stable.
 *
 * ## Example
 *
 * ### ❌ Using `stateIn` (unsafe with reusable calls)
 * ```kotlin
 * val duration: StateFlow<Duration?> =
 *     durationInMs
 *         .map { it?.toDuration(DurationUnit.SECONDS) }
 *         .stateIn(call.scope, SharingStarted.WhileSubscribed(), null)
 * ```
 * When `call.scope` is cancelled, `duration` stops updating permanently.
 *
 * ### ✅ Using [RestartableStateFlow] (safe)
 * ```kotlin
 * val duration: StateFlow<Duration?> =
 *     RestartableStateFlow(
 *         initialValue = null,
 *         upstream = durationInMs.map { it?.toDuration(DurationUnit.SECONDS) },
 *         scope = restartableProducerScope
 *     )
 * ```
 * When the call scope is recreated, upstream collection is restarted automatically
 * and `duration` continues emitting values without requiring resubscription.
 *
 * ## Key guarantees
 * - The exposed [StateFlow] instance never changes
 * - Existing collectors do not need to resubscribe
 * - Upstream collection is safely restarted across call lifecycle changes
 *
 * This class is intended for internal use where state must outlive coroutine scopes.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class RestartableStateFlow<T>(
    initialValue: T,
    upstream: Flow<T>,
    scope: RestartableProducerScope,
) : StateFlow<T> {

    private val state = MutableStateFlow(initialValue)

    init {
        scope.onAttach { realScope ->
            realScope.launch {
                upstream.collect { value ->
                    state.value = value
                }
            }
        }
    }

    override val value: T get() = state.value
    override val replayCache: List<T> get() = state.replayCache
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        state.collect(collector)
    }
}
