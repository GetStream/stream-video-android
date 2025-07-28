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

package io.getstream.video.android.core.call.connection.utils

import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.errors.RtcException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Wraps an API call and handles exceptions.
 *
 * @param apiCall The API call to wrap.
 * @return A [Result] containing the result of the API call.
 */
internal suspend fun <T : Any> wrapAPICall(apiCall: suspend () -> T): Result<T> = coroutineScope {
    withContext(coroutineContext) {
        try {
            val result = apiCall()
            Success(result)
        } catch (e: HttpException) {
            parseError(e)
        } catch (e: RtcException) {
            Failure(
                Error.ThrowableError(
                    e.message ?: "RtcException",
                    e,
                ),
            )
        } catch (e: IOException) {
            Failure(
                Error.ThrowableError(
                    e.message ?: "IOException",
                    e,
                ),
            )
        }
    }
}

/**
 * Parses the error and returns a [Failure] result.
 */
private fun parseError(e: Throwable): Failure {
    return Failure(
        Error.ThrowableError(
            "CallClientImpl error needs to be handled",
            e,
        ),
    )
}
