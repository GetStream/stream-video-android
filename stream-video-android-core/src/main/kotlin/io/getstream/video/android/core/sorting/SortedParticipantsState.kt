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

package io.getstream.video.android.core.sorting

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.pinning.PinUpdateAtTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Maintains the sorted participants list as a hot [StateFlow].
 *
 * Replaces the previous channelFlow-based implementation which closed its channel before
 * any collector subscribed (the builder block launched coroutines on the outer scope and
 * returned immediately, so subsequent trySend calls hit a closed channel). The new
 * implementation backs the public list with a [MutableStateFlow] and writes to it from
 * collectors launched on the provided [scope].
 *
 * Sorting is driven by a [SortPreset] (default [SortPreset.Default]) resolved against the
 * latest pin snapshot before each pass. Consumers can override with a custom
 * [Comparator] via [updateComparator] for ad-hoc orderings.
 */
internal class SortedParticipantsState(
    scope: CoroutineScope,
    private val call: Call,
    private val participants: StateFlow<Map<String, ParticipantState>>,
    private val pinnedParticipantsDetailed: StateFlow<Map<String, PinUpdateAtTime>>,
    initialPreset: SortPreset = SortPreset.Default,
) {
    private val logger by taggedLogger("ParticipantSorting")

    private val presetFlow = MutableStateFlow<SortPreset>(initialPreset)
    private val customComparator = MutableStateFlow<Comparator<ParticipantState>?>(null)

    private var lastSortOrder: List<String> = emptyList()

    // Serializes resort() across the two driving collectors (state combine + call events)
    // so updates to lastSortOrder + _sortedParticipants are atomic regardless of the
    // dispatcher the call's scope is running on.
    private val resortMutex = Mutex()

    private val _sortedParticipants = MutableStateFlow<List<ParticipantState>>(emptyList())
    val sortedParticipants: StateFlow<List<ParticipantState>> = _sortedParticipants

    init {
        scope.launch {
            combine(
                participants,
                pinnedParticipantsDetailed,
                presetFlow,
                customComparator,
            ) { p, pins, preset, custom ->
                SortInputs(p, pins, preset, custom)
            }.collect { resort(it) }
        }
        scope.launch {
            call.events.collect {
                resort(currentSortInputs())
            }
        }
    }

    fun setPreset(preset: SortPreset) {
        customComparator.value = null
        presetFlow.value = preset
    }

    fun updateComparator(comparator: Comparator<ParticipantState>) {
        customComparator.value = comparator
    }

    fun lastSortOrder(): List<String> = lastSortOrder

    private fun currentSortInputs(): SortInputs = SortInputs(
        participants = participants.value,
        pins = pinnedParticipantsDetailed.value,
        preset = presetFlow.value,
        custom = customComparator.value,
    )

    private suspend fun resort(inputs: SortInputs) = resortMutex.withLock {
        val comparator = inputs.custom ?: inputs.preset.build(inputs.pins)
        val ordered = orderedForSort(lastSortOrder, inputs.participants).sortedWith(comparator)
        val newOrder = ordered.map { it.sessionId }
        if (newOrder != lastSortOrder) {
            lastSortOrder = newOrder
            _sortedParticipants.value = ordered
            logger.v { "Published sorted participants (size=${ordered.size})" }
        } else {
            logger.v { "Sort produced same order, skipping emission" }
        }
    }

    /**
     * Returns participants in the previous sort order first (for stability), with any
     * newly-arrived participants appended.
     */
    private fun orderedForSort(
        previousOrder: List<String>,
        participants: Map<String, ParticipantState>,
    ): List<ParticipantState> {
        val ordered = LinkedHashMap<String, ParticipantState>(participants.size)
        previousOrder.forEach { sessionId ->
            participants[sessionId]?.let { ordered[sessionId] = it }
        }
        participants.forEach { (sessionId, state) ->
            if (sessionId !in ordered) ordered[sessionId] = state
        }
        return ordered.values.toList()
    }

    private data class SortInputs(
        val participants: Map<String, ParticipantState>,
        val pins: Map<String, PinUpdateAtTime>,
        val preset: SortPreset,
        val custom: Comparator<ParticipantState>?,
    )
}
