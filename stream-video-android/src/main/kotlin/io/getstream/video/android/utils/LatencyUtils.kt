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

import io.getstream.video.android.module.VideoModule
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * Calculates the latency to ping the server multiple times.
 *
 * @param latencyUrl The URL of the server where we ping a connection.
 * @return A [List] of [Float] values representing the portion of a second it takes to connect.
 */
public fun getLatencyMeasurements(latencyUrl: String): List<Double> {
    val measurements = mutableListOf<Double>()

    /**
     * Used for setting up testing on devices.
     */
    val url = VideoModule.REDIRECT_PING_URL ?: prepareUrl(latencyUrl)

    repeat(3) {
        val request = URL(url)
        val start = System.currentTimeMillis()
        val connection = request.openConnection()

        connection.connect()

        // Read and print the input
        val inputStream = BufferedReader(InputStreamReader(connection.getInputStream()))
        println(inputStream.readLines().toString())
        inputStream.close()

        val end = System.currentTimeMillis()

        val seconds = (end - start) / 1000.0
        measurements.add(seconds)
    }

    return measurements
}

/**
 * Prepares the URL for localhost/emulator observation.
 *
 * @param url The original URL.
 * @return [String] representation of the valid URL.
 */
internal fun prepareUrl(url: String): String =
    if (url.contains("localhost")) {
        url.replace("localhost", "10.0.2.2")
    } else {
        url
    }
