/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.utils

/**
 *  A class which encapsulates a successful outcome with a value of type [T] or a failure with [VideoError].
 */
public sealed class Result<out T : Any>

public data class Success<T : Any>(val data: T) : Result<T>()

public data class Failure(val error: VideoError) : Result<Nothing>()

/**
 * Runs the [successSideEffect] lambda function if the [Result] contains a successful data payload.
 *
 * @param successSideEffect A lambda that receives the successful data payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
public inline fun <T : Any> Result<T>.onSuccess(
    crossinline successSideEffect: (T) -> Unit,
): Result<T> = apply {
    if (this is Success) {
        successSideEffect(data)
    }
}

/**
 * Runs the suspending [successSideEffect] lambda function if the [Result] contains a successful data payload.
 *
 * @param successSideEffect A suspending lambda that receives the successful data payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
public suspend inline fun <T : Any> Result<T>.onSuccessSuspend(
    crossinline successSideEffect: suspend (T) -> Unit,
): Result<T> = apply {
    if (this is Success) {
        successSideEffect(data)
    }
}

/**
 * Runs the [errorSideEffect] lambda function if the [Result] contains an error payload.
 *
 * @param errorSideEffect A lambda that receives the [VideoError] payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
public inline fun <T : Any> Result<T>.onError(
    crossinline errorSideEffect: (VideoError) -> Unit,
): Result<T> = apply {
    if (this is Failure) {
        errorSideEffect(error)
    }
}

/**
 * Runs the suspending [errorSideEffect] lambda function if the [Result] contains an error payload.
 *
 * @param errorSideEffect A suspending lambda that receives the [VideoError] payload.
 *
 * @return The original instance of the [Result].
 */
@JvmSynthetic
public suspend inline fun <T : Any> Result<T>.onErrorSuspend(
    crossinline errorSideEffect: suspend (VideoError) -> Unit,
): Result<T> = apply {
    if (this is Failure) {
        errorSideEffect(error)
    }
}
