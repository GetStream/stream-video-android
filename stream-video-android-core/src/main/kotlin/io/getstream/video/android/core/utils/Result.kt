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

package io.getstream.video.android.core.utils

import io.getstream.video.android.core.errors.VideoError

/**
 *  A class which encapsulates a successful outcome with a value of type [T] or a failure with [VideoError].
 */
public sealed class Result<out T : Any> {

    /**
     * Checks if the result is a [Success].
     */
    public val isSuccess: Boolean
        inline get() = this is Success

    /**
     * Check if the result is a [Failure].
     */
    public val isFailure: Boolean
        inline get() = this is Failure
}

/**
 * Represents successful result.
 *
 * @param data The [T] data associated with the result.
 */
public data class Success<out T : Any>(val data: T) : Result<T>()

/**
 * Represents failed result.
 *
 * @param error The [VideoError] associated with the result.
 */
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

/**
 * Returns a transformed [Result] of applying the given [mapper] function if the [Result]
 * contains a successful data payload.
 * Returns an original [Result] if the [Result] contains an error payload.
 *
 * @param mapper A lambda for mapping [T] to [K].
 *
 * @return A transformed instance of the [Result] or the original instance of the [Result].
 */
@JvmSynthetic
public inline fun <T : Any, K : Any> Result<T>.map(mapper: (T) -> K): Result<K> =
    flatMap { Success(mapper(it)) }

/**
 * Returns a transformed [Result] of applying the given [mapper] function if the [Result]
 * contains a successful data payload.
 * Returns an original [Result] if the [Result] contains an error payload.
 *
 * @param mapper A lambda for mapping [T] to [K].
 *
 * @return A transformed instance of the [Result] or the original instance of the [Result].
 */
@JvmSynthetic
public suspend inline fun <T : Any, K : Any> Result<T>.mapSuspend(crossinline mapper: suspend (T) -> K): Result<K> =
    flatMap { Success(mapper(it)) }

/**
 * Returns a transformed [Result] of applying the given suspending [mapper] function if the [Result]
 * contains a successful data payload.
 * Returns an original [Result] if the [Result] contains an error payload.
 *
 * @param mapper A suspending lambda for mapping [T] to [Result] of [K].
 *
 * @return A transformed instance of the [Result] or the original instance of the [Result].
 */
@JvmSynthetic
public inline fun <T : Any, K : Any> Result<T>.flatMap(mapper: (T) -> Result<K>): Result<K> {
    return when (this) {
        is Success -> mapper(data)
        is Failure -> this
    }
}

/**
 * Returns a transformed [Result] from results of the suspending [mapper] if the [Result] contains a successful data
 * payload.
 * Returns an original [Result] if the [Result] contains an error payload.
 *
 * @param mapper A suspending lambda that returns [Result] of [C].
 *
 * @return A transformed instance of the [Result] or the original instance of the [Result].
 */
@JvmSynthetic
public suspend inline fun <T : Any, C : Any> Result<T>.flatMapSuspend(
    crossinline mapper: suspend (T) -> Result<C>,
): Result<C> {
    return when (this) {
        is Success -> mapper(data)
        is Failure -> this
    }
}

public fun <T : Any> Result<T>.stringify(toString: (T) -> String): String = when (this) {
    is Success -> "Success(data=${toString(data)})"
    is Failure -> toString()
}
