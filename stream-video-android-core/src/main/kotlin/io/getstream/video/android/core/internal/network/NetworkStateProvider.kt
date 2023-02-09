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

package io.getstream.video.android.core.internal.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handler which monitors connectivity and provides network state.
 *
 * @property connectivityManager Android manager which provides information about the current
 * connection state.
 */
internal class NetworkStateProvider(private val connectivityManager: ConnectivityManager) {

    private val lock: Any = Any()

    /**
     * Handler which is triggered whenever the network state changes.
     */
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            notifyListenersIfNetworkStateChanged()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            notifyListenersIfNetworkStateChanged()
        }

        override fun onLost(network: Network) {
            notifyListenersIfNetworkStateChanged()
        }
    }

    @Volatile
    private var isConnected: Boolean = isConnected()

    @Volatile
    private var listeners: Set<NetworkStateListener> = setOf()

    private val isRegistered: AtomicBoolean = AtomicBoolean(false)

    private fun notifyListenersIfNetworkStateChanged() {
        val isNowConnected = isConnected()
        if (!isConnected && isNowConnected) {
            isConnected = true
            listeners.forEach { it.onConnected() }
        } else if (isConnected && !isNowConnected) {
            isConnected = false
            listeners.forEach { it.onDisconnected() }
        }
    }

    /**
     * Checks if the current device is connected to the Internet, based on the API level.
     *
     * @return If the device is connected or not.
     */
    public fun isConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                connectivityManager.run {
                    getNetworkCapabilities(activeNetwork)?.run {
                        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    }
                }
            }.getOrNull() ?: false
        } else {
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    /**
     * Subscribes to network state changes through a listener.
     *
     * @param listener Handler which receives connection change events.
     */
    public fun subscribe(listener: NetworkStateListener) {
        synchronized(lock) {
            listeners = listeners + listener
            if (isRegistered.compareAndSet(false, true)) {
                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    callback
                )
            }
        }
    }

    /**
     * Removes a listener for network state changes.
     *
     * @param listener Handler to be removed.
     */
    public fun unsubscribe(listener: NetworkStateListener) {
        synchronized(lock) {
            listeners = (listeners - listener).also {
                if (it.isEmpty() && isRegistered.compareAndSet(true, false)) {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }
        }
    }

    /**
     * Listener which is used to listen and react to network state changes.
     */
    public interface NetworkStateListener {
        public fun onConnected()

        public fun onDisconnected()
    }
}
