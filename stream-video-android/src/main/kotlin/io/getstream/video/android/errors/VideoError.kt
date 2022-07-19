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

package io.getstream.video.android.errors

/**
 * Represents an SDK error that contains a message and the cause.
 *
 * @property message The message that represents the error.
 * @property cause Cause of the error, either a BE exception or an SDK based one.
 */
public open class VideoError(
    public val message: String? = null,
    public val cause: Throwable? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return (other as? VideoError)?.let {
            message == it.message && cause.equalCause(it.cause)
        } ?: false
    }

    private fun Throwable?.equalCause(other: Throwable?): Boolean {
        if ((this == null && other == null) || this === other) return true
        return this?.message == other?.message && this?.cause.equalCause(other?.cause)
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (cause?.hashCode() ?: 0)
        return result
    }
}
