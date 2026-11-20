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

package io.getstream.video.android.core

import kotlinx.coroutines.Job

/**
 * Prevents an older Telecom endpoint switch from being applied after a newer
 * audio device selection.
 *
 * Starting a new request cancels the previous pending endpoint wait and
 * invalidates its request token.
 */
internal class TelecomEndpointSwitchCoordinator {

    class RequestToken

    private val lock = Any()
    private var currentRequestToken: RequestToken? = null
    private var pendingEndpointWaitJob: Job? = null

    fun beginRequest(onSelected: () -> Unit): RequestToken = synchronized(lock) {
        pendingEndpointWaitJob?.cancel()
        pendingEndpointWaitJob = null
        RequestToken().also {
            currentRequestToken = it
            onSelected()
        }
    }

    fun tryRegisterPendingJob(
        requestToken: RequestToken,
        pendingEndpointWaitJob: Job,
    ): Boolean = synchronized(lock) {
        if (currentRequestToken !== requestToken) {
            false
        } else {
            this.pendingEndpointWaitJob = pendingEndpointWaitJob
            true
        }
    }

    fun runIfCurrent(requestToken: RequestToken, action: () -> Unit) {
        synchronized(lock) {
            if (currentRequestToken === requestToken) {
                action()
            }
        }
    }

    fun cancel() {
        synchronized(lock) {
            currentRequestToken = null
            pendingEndpointWaitJob?.cancel()
            pendingEndpointWaitJob = null
        }
    }
}
