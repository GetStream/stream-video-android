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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Decorator that adds thread-safety to any [PendingEventDataSource].
 *
 * Concurrency is the only concern here — all actual storage logic lives in [delegate].
 *
 * Usage:
 * ```
 * SynchronizedPendingEventDataSource(FileBasedPendingEventDataSource(storageDir))
 * SynchronizedPendingEventDataSource(InMemoryPendingEventDataSource())
 * ```
 *
 * [save] and [loadAndClear] acquire a write lock because they mutate state.
 * [isEmpty] acquires a read lock so multiple callers can check concurrently
 * without blocking each other.
 */
internal class SynchronizedPendingEventDataSource(
    private val delegate: PendingEventDataSource,
) : PendingEventDataSource {

    private val lock = ReentrantReadWriteLock()

    override fun save(events: List<ClientEvent>) = lock.write { delegate.save(events) }

    override fun loadAndClear(): List<ClientEvent> = lock.write { delegate.loadAndClear() }

    override fun isEmpty(): Boolean = lock.read { delegate.isEmpty() }
    override fun clear() {
        lock.write { delegate.clear() }
    }
}
