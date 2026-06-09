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

package io.getstream.video.android.core.analytics.reporting.datasource

import io.getstream.android.video.generated.models.ClientEvent

/**
 * Storage for telemetry events that failed to send.
 * Implementations may store events in-memory (lost on process kill)
 * or on disk (survives process restarts).
 */
internal interface PendingEventDataSource {
    fun save(events: List<ClientEvent>)
    fun loadAndClear(): List<ClientEvent>
    fun isEmpty(): Boolean
    fun clear()
}

/**
 * In-memory implementation. Events are retained within the same process session
 * and lost if the process is killed before retry.
 * For thread safety use it as
 *
 * ```
 * SynchronizedPendingEventDataSource(InMemoryPendingEventDataSource())
 * ```
 */
internal class InMemoryPendingEventDataSource : PendingEventDataSource {
    private val queue = ArrayList<ClientEvent>()

    override fun save(events: List<ClientEvent>) {
        queue.addAll(events)
    }

    override fun loadAndClear(): List<ClientEvent> = queue.toList().also { queue.clear() }

    override fun isEmpty(): Boolean = queue.isEmpty()
    override fun clear() = queue.clear()
}
