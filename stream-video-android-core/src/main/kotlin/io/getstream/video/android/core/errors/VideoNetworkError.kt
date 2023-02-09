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

package io.getstream.video.android.core.errors

public class VideoNetworkError private constructor(
    public val description: String,
    cause: Throwable? = null,
    public val streamCode: Int,
    public val statusCode: Int,
) : VideoError(
    "Status code: $statusCode, with stream code: $streamCode, description: $description",
    cause
) {

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
            (other as? VideoNetworkError)?.let {
                description == it.description &&
                    streamCode == it.streamCode &&
                    statusCode == it.statusCode
            } ?: false
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + streamCode.hashCode()
        result = 31 * result + statusCode.hashCode()
        return result
    }

    override fun toString(): String {
        return "VideoNetworkError(httpStatus=$statusCode, streamErrorCode=$streamCode:$description)"
    }

    public companion object {
        public fun create(
            code: VideoErrorCode,
            cause: Throwable? = null,
            statusCode: Int = -1
        ): VideoNetworkError {
            return VideoNetworkError(code.description, cause, code.code, statusCode)
        }

        public fun create(
            streamCode: Int,
            description: String,
            statusCode: Int,
            cause: Throwable? = null
        ): VideoNetworkError {
            return VideoNetworkError(description, cause, streamCode, statusCode)
        }
    }
}
