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

package io.getstream.video.android.core.call.scope

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope

/**
 * Provider for coroutine scopes used in RTC sessions.
 * This allows for centralized management and reuse of scopes across SDK instances.
 */
internal interface ScopeProvider {
    /**
     * Provides the main coroutine scope for RTC session operations.
     * This scope is based on the client's main scope with a supervisor job.
     */
    fun getCoroutineScope(supervisorJob: CompletableJob): CoroutineScope

    /**
     * Provides the RTC session specific scope that runs on a dedicated single thread.
     * Each call gets its own single thread executor.
     */
    fun getRtcSessionScope(supervisorJob: CompletableJob, callId: String): CoroutineScope

    /**
     * Cleans up resources when the provider is no longer needed.
     */
    fun cleanup()
}
