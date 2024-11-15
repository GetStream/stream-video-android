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

package io.getstream.video.android.core.internal.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handler which monitors connectivity and provides network state.
 *
 * @property connectivityManager Android manager which provides information about the current
 * connection state.
 */
public class NetworkStateProvider(
    private val scope: CoroutineScope,
    private val connectivityManager: ConnectivityManager,
) {

    private val logger by taggedLogger("Video:NetworkStateProvider")
    private val lock: Any = Any()
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            notifyListenersIfNetworkStateChanged()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
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
            logger.i { "Network connected." }
            isConnected = true
            listeners.onConnected()
        } else if (isConnected && !isNowConnected) {
            logger.i { "Network disconnected." }
            isConnected = false
            listeners.onDisconnected()
        }
    }

    private fun Set<NetworkStateListener>.onConnected() {
        scope.launch {
            forEach { it.onConnected() }
        }
    }

    private fun Set<NetworkStateListener>.onDisconnected() {
        scope.launch {
            forEach { it.onDisconnected() }
        }
    }

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
                safelyRegisterNetworkCallback(NetworkRequest.Builder().build(), callback)
            }
        }
    }

    /**
     * Calls [ConnectivityManager.registerNetworkCallback] and catches potential [SecurityException].
     * This is a known [bug](https://android-review.googlesource.com/c/platform/frameworks/base/+/1758029) on Android 11.
     */
    private fun safelyRegisterNetworkCallback(
        networkRequest: NetworkRequest,
        callback: ConnectivityManager.NetworkCallback,
    ) {
        connectivityManager.callWithSecurityExceptionHandling {
            registerNetworkCallback(networkRequest, callback)
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
                    safelyUnregisterNetworkCallback(callback)
                }
            }
        }
    }

    /**
     * Calls [ConnectivityManager.unregisterNetworkCallback] and catches potential [SecurityException].
     * This is a known [bug](https://android-review.googlesource.com/c/platform/frameworks/base/+/1758029) on Android 11.
     */
    private fun safelyUnregisterNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
        connectivityManager.callWithSecurityExceptionHandling {
            unregisterNetworkCallback(
                callback,
            )
        }
    }

    /**
     * Listener which is used to listen and react to network state changes.
     */
    public interface NetworkStateListener {
        public suspend fun onConnected()

        public suspend fun onDisconnected()
    }
}

private fun ConnectivityManager.callWithSecurityExceptionHandling(method: ConnectivityManager.() -> Unit) {
    try {
        method()
    } catch (e: SecurityException) {
        StreamLog.e("ConnectivityManager", e) {
            "SecurityException occurred. This is a known bug on Android 11. We log and prevent the app from crashing. Cause: ${e.message}"
        }
    }
}
