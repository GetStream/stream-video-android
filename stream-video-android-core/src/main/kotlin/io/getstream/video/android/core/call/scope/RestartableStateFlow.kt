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

package io.getstream.video.android.core.call.scope

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * A [StateFlow] whose state and collector subscriptions survive Call coroutine-scope restarts.
 *
 * ## What it is
 *
 * A regular `Flow.stateIn(callScope, ...)` permanently binds its producer to one Call scope. After
 * `Call.leave()` cancels that scope, the StateFlow object remains accessible but its upstream is no
 * longer collected. It therefore cannot produce values when the same Call instance is joined again.
 *
 * [RestartableStateFlow] separates the stable [StateFlow] storage from its producer coroutine. The
 * storage and its existing collectors remain unchanged, while this flow uses [scopeProvider] to
 * start a new producer in each Call-scope generation. A restart resets [value] to [initialValue]
 * and reconnects [upstream] using the configured [started] policy.
 *
 * ## How to create it
 *
 * Pass the upstream flow and the same [ScopeProviderImpl] that owns the reusable Call lifecycle:
 *
 * ```kotlin
 * val duration: StateFlow<Long?> = RestartableStateFlow(
 *     upstream = durationFlow,
 *     scopeProvider = scopeProvider,
 *     initialValue = null,
 *     started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000L),
 * )
 * ```
 *
 * Use this instead of `stateIn(callScope, ...)` for derived Call state that must resume producing
 * values after `leave()` followed by another `join()` on the same Call instance.
 */
@OptIn(InternalCoroutinesApi::class, ExperimentalForInheritanceCoroutinesApi::class)
internal class RestartableStateFlow<T>(
    private val upstream: Flow<T>,
    private val scopeProvider: ScopeProviderImpl,
    private val initialValue: T,
    private val started: SharingStarted,
) : StateFlow<T> {
    private val state = MutableStateFlow(initialValue)
    private var producerJob: Job? = null

    init {
        scopeProvider.onRestart(::restartProducer)
        restartProducer()
    }

    /**
     * Starts the upstream producer in the current Call scope while preserving the configured
     * [SharingStarted] behaviour of `Flow.stateIn`.
     *
     * Collecting a `shareIn` result into [state] would introduce a permanent internal subscriber.
     * With `SharingStarted.WhileSubscribed`, that subscriber would keep [upstream] active even when
     * nobody is collecting this StateFlow. Instead, the sharing commands are derived from the
     * actual subscribers of [state]: [SharingCommand.START] collects [upstream],
     * [SharingCommand.STOP] stops that collection, and
     * [SharingCommand.STOP_AND_RESET_REPLAY_CACHE] restores [initialValue]. [collectLatest]
     * cancels the previous command's upstream collection whenever the sharing command changes.
     */
    private fun restartProducer() {
        producerJob?.cancel()
        state.value = initialValue
        producerJob = scopeProvider.callScope.launch {
            started.command(state.subscriptionCount).distinctUntilChanged().collectLatest { command ->
                when (command) {
                    SharingCommand.START -> upstream.collect { state.value = it }
                    SharingCommand.STOP -> Unit
                    SharingCommand.STOP_AND_RESET_REPLAY_CACHE -> state.value = initialValue
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
