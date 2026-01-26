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

package io.getstream.video.android.core.utils

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit

data class LatencyResult(
    val latencyUrl: String,
    val measurements: List<Float> = emptyList(),
    val average: Double = 100.0,
    val failed: Throwable? = null,
)

public fun getLatencyMeasurementsOKHttp(latencyUrl: String): LatencyResult {
    val measurements = mutableListOf<Float>()
    val connectionTimeoutInMs: Long = 3000

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .build()

    try {
        repeat(3) {
            val start = System.currentTimeMillis()

            val okHttpRequest: Request = Request.Builder()
                .url(latencyUrl) // 2-second response time
                .build()
            val call: Call = client.newCall(okHttpRequest)
            val response: Response = call.execute()

            val end = System.currentTimeMillis()
            val seconds = (end - start) / 1000f
            if (it != 0) {
                measurements.add(seconds)
            }
        }
    } catch (e: Throwable) {
        measurements.add(Float.MAX_VALUE)
        return LatencyResult(latencyUrl, measurements, measurements.average(), e)
    }
    return LatencyResult(latencyUrl, measurements, measurements.average())
}

/**
 * Calculates the latency to ping the server multiple times.
 *
 * @param latencyUrl The URL of the server where we ping a connection.
 * @return A [List] of [Double] values representing the portion of a second it takes to connect.
 */
public fun getLatencyMeasurements(latencyUrl: String): LatencyResult {
    val measurements = mutableListOf<Float>()
    val connectionTimeoutInMs: Long = 3000

    try {
        repeat(3) {
            val request = URL(latencyUrl)
            val start = System.currentTimeMillis()
            val connection = request.openConnection()
            // ensure we have a timeout
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()

            // Read and print the input
            val inputStream = BufferedReader(InputStreamReader(connection.getInputStream()))
            inputStream.close()

            val end = System.currentTimeMillis()
            val seconds = (end - start) / 1000f
            if (it != 0) {
                measurements.add(seconds)
            }
        }
    } catch (e: Throwable) {
        measurements.add(Float.MAX_VALUE)
        return LatencyResult(latencyUrl, measurements, measurements.average(), e)
    }
    return LatencyResult(latencyUrl, measurements, measurements.average())
}
