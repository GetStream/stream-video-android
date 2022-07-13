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

package io.getstream.video.android.parser

import io.getstream.video.android.errors.VideoErrorCode
import io.getstream.video.android.errors.VideoNetworkError
import io.getstream.video.android.socket.ErrorResponse
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.VideoError
import okhttp3.Response
import okhttp3.ResponseBody

internal interface VideoParser {

    private val tag: String
        get() = VideoParser::class.java.simpleName

    fun toJson(any: Any): String
    fun <T : Any> fromJson(raw: String, clazz: Class<T>): T

    @Suppress("TooGenericExceptionCaught")
    fun <T : Any> fromJsonOrError(raw: String, clazz: Class<T>): Result<T> {
        return try {
            Success(fromJson(raw, clazz))
        } catch (expected: Throwable) {
            Failure(VideoError("fromJsonOrError error parsing of $clazz into $raw", expected))
        }
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    fun toError(okHttpResponse: Response): VideoNetworkError {
        val statusCode: Int = okHttpResponse.code

        return try {
            // Try to parse default Stream error body
            val body = okHttpResponse.peekBody(Long.MAX_VALUE).string()

            if (body.isEmpty()) {
                VideoNetworkError.create(VideoErrorCode.NO_ERROR_BODY, statusCode = statusCode)
            } else {
                val error = try {
                    fromJson(body, ErrorResponse::class.java)
                } catch (_: Throwable) {
                    ErrorResponse().apply { message = body }
                }
                VideoNetworkError.create(
                    streamCode = error.code,
                    description = error.message + moreInfoTemplate(error.moreInfo),
                    statusCode = statusCode
                )
            }
        } catch (expected: Throwable) {
            VideoNetworkError.create(
                code = VideoErrorCode.NETWORK_FAILED,
                cause = expected,
                statusCode = statusCode
            )
        }
    }

    fun toError(errorResponseBody: ResponseBody): VideoNetworkError {
        return try {
            val errorResponse: ErrorResponse =
                fromJson(errorResponseBody.string(), ErrorResponse::class.java)
            val (code, message, statusCode, _, moreInfo) = errorResponse

            VideoNetworkError.create(
                streamCode = code,
                description = message + moreInfoTemplate(moreInfo),
                statusCode = statusCode
            )
        } catch (expected: Throwable) {
            VideoNetworkError.create(
                code = VideoErrorCode.NETWORK_FAILED,
                cause = expected,
                statusCode = -1
            )
        }
    }

    private fun moreInfoTemplate(moreInfo: String): String {
        return if (moreInfo.isNotBlank()) {
            "\nMore information available at $moreInfo"
        } else ""
    }
}
