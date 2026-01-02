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

package io.getstream.video.android.core.notifications.internal

import io.getstream.log.taggedLogger
import java.util.concurrent.ConcurrentHashMap
import kotlin.getValue

internal object Throttler {

    private val logger by taggedLogger("Throttler")

    // A thread-safe map to store the last execution time for each key.
    // The value is the timestamp (in milliseconds) when the key's cooldown started.
    private val lastExecutionTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * Submits an action for potential execution, identified by a unique key.
     *
     * @param key A unique String identifying this action or instruction.
     * @param cooldownMs The duration in milliseconds for this key's cooldown period.
     * @param action The lambda to execute if the key is not on cooldown.
     */
    fun throttleFirst(key: String, cooldownMs: Long, action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        val lastExecutionTime = lastExecutionTimestamps[key] ?: 0L
        val timeDiff = currentTime - lastExecutionTime

        // Check if the key is not on cooldown.
        // This is true if the key has never been used (lastExecutionTime is null)
        // or if the cooldown period has passed.
        logger.d {
            "[throttleFirst], timeDiff: $timeDiff, current: $currentTime, lastExecutionTime: $lastExecutionTime, key:$key, hashcode: ${hashCode()}"
        }
        if (lastExecutionTime == 0L || (timeDiff) >= cooldownMs) {
            // Update the last execution time for this key to the current time.
            lastExecutionTimestamps[key] = currentTime
            // Execute the action.
            action()
        }
        // If the key is on cooldown, do nothing.
    }

    fun throttleFirst(cooldownMs: Long, action: () -> Unit) {
        val key = getKey(action)
        throttleFirst(key, cooldownMs, action)
    }

    fun getKey(action: () -> Unit): String {
        return Thread.currentThread().stackTrace.getOrNull(4)?.let {
            "${it.className}#${it.methodName}:${it.lineNumber}"
        } ?: "fallback_${action.hashCode()}"
    }

    /**
     * Manually clears the cooldown for a specific key, allowing its next action to run immediately.
     *
     * @param key The key to reset.
     */
    fun reset(key: String) {
        lastExecutionTimestamps.remove(key)
    }

    /**
     * Clears all active cooldowns.
     */
    fun resetAll() {
        lastExecutionTimestamps.clear()
    }
}
