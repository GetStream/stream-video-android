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

package io.getstream.video.android.core.coroutines.scopes

import io.getstream.video.android.core.internal.InternalStreamVideoApi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A lifecycle-aware producer scope used to run long-lived or state-producing coroutines
 * that must survive call leave/join cycles.
 *
 * Unlike a regular [CoroutineScope], this scope does not represent a fixed lifecycle.
 * Instead, it forwards coroutine execution to the currently active call scope and
 * allows producers to be explicitly restarted when the call scope is recreated.
 *
 * This is required to safely reuse call- and participant-level state after `call.leave()`,
 * where the original coroutine scope is cancelled and replaced.
 *
 * This parameter is intended for internal use only.
 */
@InternalStreamVideoApi
public class RestartableProducerScope : CoroutineScope {

    @Volatile
    private var currentScope: CoroutineScope? = null

    private val onAttachCallbacks = mutableListOf<(CoroutineScope) -> Unit>()

    internal fun attach(scope: CoroutineScope) {
        currentScope = scope
        onAttachCallbacks.forEach { it(scope) }
    }

    internal fun detach() {
        currentScope = null
    }

    override val coroutineContext: CoroutineContext
        get() = currentScope?.coroutineContext ?: EmptyCoroutineContext

    internal fun onAttach(block: (CoroutineScope) -> Unit) {
        onAttachCallbacks += block
        currentScope?.let { block(it) } // start immediately if already attached
    }
}
