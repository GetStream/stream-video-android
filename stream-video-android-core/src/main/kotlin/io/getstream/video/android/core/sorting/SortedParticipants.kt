/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.core.events.VideoEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.openapitools.client.models.VideoEvent
import org.threeten.bp.OffsetDateTime
import java.util.SortedMap

class SortedParticipants(
    scope: CoroutineScope,
    private val call: Call,
    private val participants: MutableStateFlow<SortedMap<String, ParticipantState>>,
    private val pinnedParticipants: MutableStateFlow<Map<String, OffsetDateTime>>,
    private var comparator: Comparator<ParticipantState>? = null,
) {
    // Internal
    private val logger by taggedLogger("ParticipantSorting")
    private val lastSortOrder: MutableList<String> = mutableListOf()
    private val internalFlow: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(emptyList())
    private val callEventsListener = VideoEventListener<VideoEvent> {
        logger.v { "Call event received ${it.getEventType()}, updating sort order" }
        sortAndPublish(participants.value, pinnedParticipants.value)
    }

    init {
        scope.launch {
            // Respond to call events
            call.subscribe(callEventsListener)
            // Subscribe to participants and pinning
            combine(participants, pinnedParticipants) { p1, p2 ->
                Pair(p1, p2)
            }.collectLatest {
                sortAndPublish(it.first, it.second)
            }
        }
    }

    // Internal logic
    private fun sortAndPublish(
        participants: Map<String, ParticipantState>,
        pinnedParticipants: Map<String, OffsetDateTime>,
    ) {
        val sorted = internalSort(participants.values.toSet(), pinnedParticipants.keys.toSet())
        val newOrder = sorted.map { it.sessionId }
        val differentOrder = lastSortOrder != newOrder
        if (differentOrder) {
            updateLastSortOrder(sorted)
            // Publish
            logger.v { "Publishing sorted participants." }
            internalFlow.value = sorted
        } else {
            // Order is same
            logger.v { "Order and participants are the same, do not publish." }
        }
    }

    private fun internalSort(
        participants: Set<ParticipantState>,
        pinned: Set<String>,
    ): List<ParticipantState> {
        logger.v {
            "Sorting ${participants.size} participants. (diff: ${participants.size - lastSortOrder.size})"
        }
        val currentSortOrder = determineCurrentSortOrder(lastSortOrder, participants)
        val resolvedComparator = comparator ?: defaultComparator(pinned)
        return currentSortOrder.sortedWith(resolvedComparator)
    }

    private fun determineCurrentSortOrder(lastSortOrder: List<String>, participants: Set<ParticipantState>): List<ParticipantState> {
        val map = linkedMapOf<String, ParticipantState>()
        participants.forEach {
            map[it.sessionId] = it
        }
        val sortedMap = linkedMapOf<String, ParticipantState>()
        lastSortOrder.forEach { key ->
            val participant = map[key]
            participant?.let {
                sortedMap[key] = it
            }
        }
        participants.forEach {
            sortedMap[it.sessionId] = it
        }
        return sortedMap.values.toList()
    }

    private fun updateLastSortOrder(newSortOrder: List<ParticipantState>) {
        val newSortOrderIds = newSortOrder.map {
            it.sessionId
        }
        lastSortOrder.clear()
        lastSortOrder.addAll(newSortOrderIds)
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
}
