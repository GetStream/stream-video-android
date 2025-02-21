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

package io.getstream.video.android.core.socket.common

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.StreamLog
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.extractCause
import io.getstream.result.recover
import io.getstream.video.android.core.call.signal.socket.RTCEventMapper
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.SfuDataRequest
import okhttp3.Response
import okio.ByteString
import okio.ByteString.Companion.toByteString
import retrofit2.Retrofit
import stream.video.sfu.event.SfuEvent
import stream.video.sfu.models.WebsocketReconnectStrategy

internal interface GenericParser<E> {
    /** Encodes the given object into a ByteString. */
    fun encode(event: E): ByteString

    /** Decodes the given [raw] [ByteArray] into an object of type [T]. */
    fun decodeOrError(raw: ByteArray): Result<StreamWebSocketEvent>
}

internal interface SfuParser : GenericParser<SfuDataRequest> {

    private val tag: String get() = "Video:SfuParser"
    override fun encode(event: SfuDataRequest): ByteString {
        return event.sfuRequest.encodeByteString()
    }

    override fun decodeOrError(raw: ByteArray): Result<StreamWebSocketEvent> {
        try {
            val event = SfuEvent.ADAPTER.decode(raw)
            if (event.error != null) {
                val errorEvent = RTCEventMapper.mapEvent(event)
                return toError(errorEvent as ErrorEvent)
            }
            return Result.Success(StreamWebSocketEvent.SfuMessage(RTCEventMapper.mapEvent(event)))
        } catch (e: Exception) {
            val error = Error.NetworkError.fromVideoErrorCode(
                VideoErrorCode.UNABLE_TO_PARSE_SOCKET_EVENT,
                cause = e,
            )
            return Result.Failure(error)
        }
    }

    fun toError(errorEvent: ErrorEvent?): Result<StreamWebSocketEvent> {
        if (errorEvent != null) {
            val serverError = errorEvent.error
            val streamError = serverError?.let {
                Error.NetworkError(
                    message = it.message,
                    statusCode = it.code.value,
                    serverErrorCode = it.code.value,
                )
            } ?: let {
                Error.NetworkError.fromVideoErrorCode(VideoErrorCode.UNABLE_TO_PARSE_SOCKET_EVENT)
            }
            val reconnectStrategy = errorEvent.reconnectStrategy
            return Result.Success(StreamWebSocketEvent.Error(streamError, reconnectStrategy))
        } else {
            StreamLog.e(tag) { "[toError] failed" }
            val error = Error.NetworkError.fromVideoErrorCode(VideoErrorCode.CANT_PARSE_EVENT)
            return Result.Success(
                StreamWebSocketEvent.Error(
                    error,
                    WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                ),
            )
        }
    }
}

internal interface VideoParser : GenericParser<VideoEvent> {

    private val tag: String get() = "Video:VideoParser"
    override fun encode(event: VideoEvent): ByteString {
        val json = toJson(event)
        return json.encodeToByteArray().toByteString()
    }

    override fun decodeOrError(raw: ByteArray): Result<StreamWebSocketEvent> {
        val text = String(raw)
        StreamLog.d(tag) { "[decode] raw: ${String(raw)}" }
        val event = fromJsonOrError(text, VideoEvent::class.java)
            .map { StreamWebSocketEvent.VideoMessage(it) }
            .recover { parseChatError ->
                StreamLog.d(tag) { "[decode#recover] raw: $parseChatError" }
                val errorResponse =
                    when (
                        val chatErrorResult = fromJsonOrError(
                            text,
                            SocketErrorMessage::class.java,
                        )
                    ) {
                        is Result.Success -> {
                            chatErrorResult.value.error
                        }
                        is Result.Failure -> null
                    }
                StreamWebSocketEvent.Error(
                    errorResponse?.let {
                        Error.NetworkError(
                            message = it.message,
                            statusCode = it.statusCode,
                            serverErrorCode = it.code,
                        )
                    } ?: Error.NetworkError.fromVideoErrorCode(
                        videoErrorCode = VideoErrorCode.CANT_PARSE_EVENT,
                        cause = parseChatError.extractCause(),
                    ),
                )
            }.value

        return Result.Success(event)
    }

    fun toJson(any: Any): String
    fun <T : Any> fromJson(raw: String, clazz: Class<T>): T
    fun configRetrofit(builder: Retrofit.Builder): Retrofit.Builder

    @Suppress("TooGenericExceptionCaught")
    fun <T : Any> fromJsonOrError(raw: String, clazz: Class<T>): Result<T> {
        return try {
            Result.Success(fromJson(raw, clazz))
        } catch (expected: Throwable) {
            Result.Failure(
                Error.ThrowableError("fromJsonOrError error parsing of $clazz into $raw", expected),
            )
        }
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    fun toError(okHttpResponse: Response): Error.NetworkError {
        val statusCode: Int = okHttpResponse.code

        return try {
            // Try to parse default Stream error body
            val body = okHttpResponse.peekBody(Long.MAX_VALUE).string()

            if (body.isEmpty()) {
                Error.NetworkError.fromVideoErrorCode(
                    videoErrorCode = VideoErrorCode.NO_ERROR_BODY,
                    statusCode = statusCode,
                )
            } else {
                val error = try {
                    fromJson(body, ErrorResponse::class.java)
                } catch (_: Throwable) {
                    ErrorResponse().apply { message = body }
                }
                Error.NetworkError(
                    serverErrorCode = error.code,
                    message = error.message +
                        moreInfoTemplate(error.moreInfo) +
                        buildDetailsTemplate(error.details),
                    statusCode = statusCode,
                )
            }
        } catch (expected: Throwable) {
            StreamLog.e(tag, expected) { "[toError] failed" }
            Error.NetworkError.fromVideoErrorCode(
                videoErrorCode = VideoErrorCode.NETWORK_FAILED,
                cause = expected,
                statusCode = statusCode,
            )
        }
    }

    private fun moreInfoTemplate(moreInfo: String): String {
        return if (moreInfo.isNotBlank()) {
            "\nMore information available at $moreInfo"
        } else {
            ""
        }
    }

    private fun buildDetailsTemplate(details: List<ErrorDetail>): String {
        return if (details.isNotEmpty()) {
            "\nError details: $details"
        } else {
            ""
        }
    }
}

public fun Error.NetworkError.Companion.fromVideoErrorCode(
    videoErrorCode: VideoErrorCode,
    statusCode: Int = UNKNOWN_STATUS_CODE,
    cause: Throwable? = null,
): Error.NetworkError {
    return Error.NetworkError(
        message = videoErrorCode.description,
        serverErrorCode = videoErrorCode.code,
        statusCode = statusCode,
        cause = cause,
    )
}
