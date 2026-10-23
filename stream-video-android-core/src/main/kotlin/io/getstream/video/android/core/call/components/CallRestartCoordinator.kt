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

package io.getstream.video.android.core.call.components

import io.getstream.video.android.core.call.Restartable

/** Coordinates restoration of a Call after its previous lifecycle generation has ended. */
internal class CallRestartCoordinator(
    private val scopeProvider: Restartable,
    private val restartables: List<Restartable>,
    private val onRestarted: () -> Unit = {},
) {
    /** Restarts generation-owned resources first, then each persistent component in order. */
    fun restart() {
        scopeProvider.restart()
        restartables.forEach(Restartable::restart)
        onRestarted()
    }
}
