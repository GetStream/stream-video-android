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

package io.getstream.video.android.core.call.connection.utils

import io.getstream.log.StreamLog
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import kotlin.coroutines.cancellation.CancellationException

/**
 * Wraps an SFU API call, catching all exceptions except [CancellationException]
 * (which is rethrown to preserve structured concurrency).
 *
 * Replaces the previous mix of `wrapAPICall`, `safeCall`, and
 * `safeCallWithResult` with a single, consistent wrapper for all SFU
 * signaling calls.
 *
 * @return [Success] if [block] completed, [Failure] if it threw.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend inline fun <T : Any> sfuCall(
    crossinline block: suspend () -> T,
): Result<T> {
    return try {
        Success(block())
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        StreamLog.e("SfuCall", t) { "SFU API call failed: ${t.message}" }
        Failure(Error.ThrowableError(t.message ?: "SFU call failed", t))
    }
}
