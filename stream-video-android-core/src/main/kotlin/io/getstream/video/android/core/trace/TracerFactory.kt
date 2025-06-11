/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.trace

import java.util.concurrent.ConcurrentHashMap

/**
 * Factory that provides [Tracer] instances.
 */
class TracerFactory(private var enabled: Boolean = true) {
    /**
     * All tracers created by this factory.
     */
    private val tracers = ConcurrentHashMap<String, Tracer>()

    fun tracers(): List<Tracer> {
        return tracers.values.toList()
    }

    /**
     * Returns a [Tracer] for the given [name].
     *
     * @param name The name of the tracer.
     * @return [Tracer] for the given [name].
     */
    fun tracer(name: String): Tracer {
        return tracers.getOrPut(name) { Tracer(name).also { it.setEnabled(enabled) } }
    }

    /** Clears all tracers. */
    fun clear() {
        tracers.clear()
    }

    /**
     * Enables or disables tracing.
     *
     * @param enabled True if tracing should be enabled, false otherwise.
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        tracers.values.forEach { it.setEnabled(enabled) }
    }
}
