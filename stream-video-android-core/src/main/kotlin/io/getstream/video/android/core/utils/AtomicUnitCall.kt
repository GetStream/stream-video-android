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

package io.getstream.video.android.core.utils

import java.util.concurrent.atomic.AtomicBoolean

/**
 * An atomic call that is executed only once, no matter how many consecutive calls are made.
 */
internal class AtomicUnitCall {
    private var called = AtomicBoolean(false)

    /**
     * Executes the given block of code only once, no matter how many consecutive calls are made.
     */
    operator fun invoke(block: () -> Unit) {
        if (called.compareAndSet(false, true)) {
            safeCall {
                block()
            }
        }
    }
}
