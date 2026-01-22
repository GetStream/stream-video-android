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

package io.getstream.video.android.core.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex

internal class CallLocks(clientScope: CoroutineScope) {
    internal val cleanupMutex = Mutex()
    internal var cleanupJob: Job? = null

    @Volatile
    internal var currentSupervisorJob: Job = SupervisorJob()

    @Volatile
    internal var currentScope: CoroutineScope =
        CoroutineScope(clientScope.coroutineContext + currentSupervisorJob)
}
