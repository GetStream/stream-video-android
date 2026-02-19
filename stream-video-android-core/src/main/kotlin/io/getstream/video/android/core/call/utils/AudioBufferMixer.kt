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

package io.getstream.video.android.core.call.utils

import java.nio.ByteBuffer

/**
 * Mixes two audio buffers by adding their samples together and converting to ByteArray.
 * The buffers are expected to be in PCM 16-bit format.
 *
 * @param a1 First audio buffer as ShortArray
 * @param a1Limit Number of samples to use from a1
 * @param a2 Second audio buffer as ShortArray
 * @param a2Limit Number of samples to use from a2
 * @return Mixed audio buffer as ByteArray in little-endian format
 */
internal fun addAndConvertBuffers(
    a1: ShortArray,
    a1Limit: Int,
    a2: ShortArray,
    a2Limit: Int,
): ByteBuffer {
    val size = Math.max(a1Limit, a2Limit)
    if (size < 0) return ByteBuffer.wrap(ByteArray(0))
    val buff = ByteArray(size * 2)
    for (i in 0 until size) {
        var sum: Int
        sum = if (i >= a1Limit) {
            a2[i].toInt()
        } else if (i >= a2Limit) {
            a1[i].toInt()
        } else {
            a1[i].toInt() + a2[i].toInt()
        }
        if (sum > Short.MAX_VALUE) sum = Short.MAX_VALUE.toInt()
        if (sum < Short.MIN_VALUE) sum = Short.MIN_VALUE.toInt()
        val byteIndex = i * 2
        buff[byteIndex] = (sum and 0xff).toByte()
        buff[byteIndex + 1] = (sum shr 8 and 0xff).toByte()
    }
    return ByteBuffer.wrap(buff)
}
