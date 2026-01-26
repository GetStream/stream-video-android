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

package io.getstream.video.android.util

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

class InstallReferrer(val context: Context) {

    private val sharedPreference =
        context.getSharedPreferences("install_referrer", Context.MODE_PRIVATE)

    /**
     * This function is used to extract the "call_id" value forwarded from the QR code
     * in our demo flow. The user can be automatically forwarded to the call after
     * the app is installed from the QR code Google Play link.
     * Execute this function in the main entry Activity. This function can be executed
     * on each start - it will only try to read and return the value on first start. Subsequent
     * calls even after a process kill will be ignored.
     */
    fun extractInstallReferrer(callIdCallback: (String) -> Unit) {
        // The Install Referrer will give us the same value repeatedly. Only attempt to read
        // it on first start.
        if (sharedPreference.getBoolean(PREF_REFERRER_WAS_READ_BOOLEAN, false)) {
            return
        }

        val referrerClient: InstallReferrerClient = InstallReferrerClient.newBuilder(
            context,
        ).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        val referrer: String = referrerClient.installReferrer.installReferrer
                        val callIdValue = findValue(referrer, "call_id")
                        if (!callIdValue.isNullOrEmpty()) {
                            callIdCallback.invoke(callIdValue)
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        // Connection couldn't be established.
                    }
                }

                // close the connection after we got a response (we will not try again)
                kotlin.runCatching { referrerClient.endConnection() }

                sharedPreference.edit().putBoolean(PREF_REFERRER_WAS_READ_BOOLEAN, true).apply()
            }

            override fun onInstallReferrerServiceDisconnected() { }
        })
    }

    private fun findValue(referrer: String, key: String): String? {
        referrer.split("&").forEach {
            val keyValueList = it.split("=")
            if (keyValueList.size > 1 && keyValueList[0] == key) {
                return keyValueList[1]
            }
        }
        return null
    }

    private companion object {
        const val PREF_REFERRER_WAS_READ_BOOLEAN = "PREF_REFERRER_WAS_READ_BOOLEAN"
    }
}
