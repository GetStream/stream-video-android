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

import io.getstream.log.StreamLog
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

private const val TAG = "Call:LatencyUtils"

/**
 * Calculates the latency to ping the server multiple times.
 *
 * @param latencyUrl The URL of the server where we ping a connection.
 * @return A [List] of [Double] values representing the portion of a second it takes to connect.
 */
public fun getLatencyMeasurements(latencyUrl: String): List<Float> {
    val measurements = mutableListOf<Float>()

    /**
     * Used for setting up testing on devices.
     */
    val url = latencyUrl

    repeat(3) {
        try {
            val request = URL(url)
            val start = System.currentTimeMillis()
            val connection = request.openConnection()

            connection.connect()

            // Read and print the input
            val inputStream = BufferedReader(InputStreamReader(connection.getInputStream()))
            println(inputStream.readLines().toString())
            inputStream.close()

            val end = System.currentTimeMillis()

            val seconds = (end - start) / 1000f
            measurements.add(seconds)
        } catch (e: Throwable) {
            StreamLog.e(TAG, e) { "[getLatencyMeasurements] failed: $e" }
            measurements.add(Float.MAX_VALUE)
        }
    }
    return measurements
}
