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
 * Keeps state storage and subscriber identity across call scopes. Sharing commands are driven
 * by the exposed state's subscribers, so WhileSubscribed does not collect without subscribers.
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

    private fun restartProducer() {
        producerJob?.cancel()
        state.value = initialValue
        // TODO: Revisit whether this manual SharingStarted implementation can be simplified.
        //  It currently uses the exposed StateFlow's subscriber count to preserve stateIn's
        //  WhileSubscribed and replay-reset behavior across scope restarts.
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

internal fun <T> Flow<T>.restartableStateIn(
    scopeProvider: ScopeProviderImpl,
    started: SharingStarted,
    initialValue: T,
): RestartableStateFlow<T> = RestartableStateFlow(this, scopeProvider, initialValue, started)
