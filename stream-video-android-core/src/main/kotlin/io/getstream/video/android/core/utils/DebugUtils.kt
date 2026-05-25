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

import io.getstream.video.android.core.BuildConfig

/**
 * Executes [block] only when [BuildConfig.DEBUG_TOOLS_ENABLED] is `true` (debug builds).
 * In release builds the call is a no-op.
 */
internal inline fun debugOnly(block: () -> Unit) {
    if (BuildConfig.DEBUG_TOOLS_ENABLED) {
        block()
    }
}

/**
 * Suspend variant of [debugOnly] for coroutine contexts.
 */
internal suspend inline fun suspendDebugOnly(block: suspend () -> Unit) {
    if (BuildConfig.DEBUG_TOOLS_ENABLED) {
        block()
    }
}
