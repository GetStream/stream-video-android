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

package io.getstream.video.android.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal class StreamLifecycleObserver(
    private val scope: CoroutineScope,
    private val lifecycle: Lifecycle,
) : DefaultLifecycleObserver {

    private val logger by taggedLogger("Video:LifecycleObserver")
    private var recurringResumeEvent = false
    private var handlers = setOf<LifecycleHandler>()

    private val isObserving = AtomicBoolean(false)

    suspend fun observe(handler: LifecycleHandler) {
        if (isObserving.compareAndSet(false, true)) {
            recurringResumeEvent = false
            withContext(DispatcherProvider.Main) {
                lifecycle.addObserver(this@StreamLifecycleObserver)
                logger.v { "[observe] subscribed" }
            }
        }
        handlers = handlers + handler
    }

    suspend fun dispose(handler: LifecycleHandler) {
        handlers = handlers - handler
        if (handlers.isEmpty() && isObserving.compareAndSet(true, false)) {
            withContext(DispatcherProvider.Main) {
                lifecycle.removeObserver(this@StreamLifecycleObserver)
                logger.v { "[dispose] unsubscribed" }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        logger.d { "[onResume] owner: $owner, recurringResumeEvent: $recurringResumeEvent" }
        // ignore event when we just started observing the lifecycle
        if (recurringResumeEvent) {
            scope.launch {
                handlers.forEach { it.resume() }
            }
        }
        recurringResumeEvent = true
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            logger.d { "[onStop] owner: $owner" }
            handlers.forEach { it.stopped() }
        }
    }
}

internal interface LifecycleHandler {
    suspend fun resume()
    suspend fun stopped()
}
