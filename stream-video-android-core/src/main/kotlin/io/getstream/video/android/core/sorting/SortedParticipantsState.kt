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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

/**
 * A state that takes care of book-keeping participants and its sort order.
 *
 * @param scope the coroutine scope where
 * @param call the call for which the participants are sorted
 * @param participants the flow of participants
 * @param pinnedParticipants the pinned participants flow
 * @param comparator the comparator
 */
internal class SortedParticipantsState(
    scope: CoroutineScope,
    private val call: Call,
    private val participants: MutableStateFlow<Map<String, ParticipantState>>,
    private val pinnedParticipants: StateFlow<Map<String, OffsetDateTime>>,
    private var comparator: Comparator<ParticipantState>? = null,
) {
    // Internal
    private val logger by taggedLogger("ParticipantSorting")
    private var lastSortOrder: List<String> = emptyList()
    private val internalFlow: Flow<List<ParticipantState>>

    init {
        internalFlow = channelFlow {
            // Respond to call events
            scope.launch {
                call.events.collectLatest {
                    sortAndPublish(lastSortOrder, participants.value, pinnedParticipants.value)
                }
            }

            // Combine the participants and pinned flow
            scope.launch {
                combine(participants, pinnedParticipants) { p1, p2 ->
                    Pair(p1, p2)
                }.collectLatest {
                    sortAndPublish(lastSortOrder, it.first, it.second)
                }
            }
        }
    }

    // Internal logic
    private fun ProducerScope<List<ParticipantState>>.sortAndPublish(
        previousSortOrder: List<String>,
        participants: Map<String, ParticipantState>,
        pinnedParticipants: Map<String, OffsetDateTime>,
    ) {
        // Sort
        val sorted = internalSort(previousSortOrder, participants, pinnedParticipants.keys)
        val newOrder = sorted.map { it.sessionId }
        val differentOrder = previousSortOrder != newOrder

        if (differentOrder) {
            // Update last sort order
            lastSortOrder = newOrder
            // Publish
            logger.v { "Publishing sorted participants." }
            trySend(sorted) // Try send instead of send.
        } else {
            // Order is same
            logger.v { "Order and participants are the same, do not publish." }
        }
    }

    private fun internalSort(
        previousSortOrder: List<String>,
        participants: Map<String, ParticipantState>,
        pinned: Set<String>,
    ): List<ParticipantState> {
        val currentSortOrder = determineCurrentSortOrder(previousSortOrder, participants)
        val resolvedComparator = comparator ?: defaultComparator(pinned)
        return currentSortOrder.sortedWith(resolvedComparator)
    }

    private fun determineCurrentSortOrder(
        previousSortOrder: List<String>,
        participants: Map<String, ParticipantState>,
    ): List<ParticipantState> {
        val sortedMap = linkedMapOf<String, ParticipantState>()
        // Add participants in the sortedMap according to the last sort order,
        // only if they exist in participant list
        previousSortOrder.forEach { key ->
            participants[key]?.let {
                sortedMap[key] = it
            }
        }
        // Add any remaining participants at the end
        participants.forEach { (key, value) ->
            if (!sortedMap.containsKey(key)) {
                sortedMap[key] = value
            }
        }
        return sortedMap.values.toList()
    }

    // API
    /**
     * Allows to get the internal flow in order to subscribe and receive updates to the sorted participants.
     */
    fun asFlow(): Flow<List<ParticipantState>> = internalFlow

    /**
     * Get last sorted participants.
     */
    fun lastSortOrder() = lastSortOrder

    /**
     * Update the sorter with a new comparator.
     */
    fun updateComparator(comparator: Comparator<ParticipantState>) {
        this.comparator = comparator
    }
}
