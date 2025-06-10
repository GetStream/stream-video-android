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