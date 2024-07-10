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

package io.getstream.video.android.core.socket.common.scope

import android.util.Log
import io.getstream.result.call.SharedCalls
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A client aware implementation of [CoroutineScope].
 */
internal interface ClientScope : CoroutineScope

/**
 * Creates a client aware [CoroutineScope].
 */
internal fun ClientScope(): ClientScope = ClientScopeImpl()

/**
 * Represents SDK root [CoroutineScope].
 */
private class ClientScopeImpl :
    ClientScope,
    CoroutineScope by CoroutineScope(
        SupervisorJob() + DispatcherProvider.IO + SharedCalls(),
    )

/**
 * Launches a coroutine and catches any exception. */
fun CoroutineScope.safeLaunch(block: suspend () -> Unit) {
    launch {
        try {
            block()
        } catch (e: Throwable) {
            Log.e("ClientScope", "Error in coroutine", e)
        }
    }
}
