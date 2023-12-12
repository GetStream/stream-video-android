/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import io.getstream.log.taggedLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class NetworkConnectivityHelper(context: Context) {
    private val connectivityManager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE,
    ) as ConnectivityManager
    private val logger by taggedLogger("NetworkConnectivity")

    fun observe(): Flow<Boolean> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(true) }
                    logger.i { "Network available" }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch { send(false) }
                    logger.i { "Network lost" }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch { send(false) }
                    logger.i { "Network unavailable" }
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)

            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }
}
