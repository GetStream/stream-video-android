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

package io.getstream.video.android.core.header

import android.content.Context
import android.os.Build
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.InternalStreamVideoApi

@InternalStreamVideoApi
class SdkTrackingHeaders {
    /**
     * Header used to track which SDK is being used.
     */
    companion object {
        @InternalStreamVideoApi
        @JvmStatic
        public var VERSION_PREFIX_HEADER: VersionPrefixHeader = VersionPrefixHeader.Default
    }

    /**
     * Retrieves the version name of the application.
     *
     * @return The version name (e.g., "1.2.3") or `"nameNotFound"` if retrieval fails.
     */
    private fun getAppVersionName(): String {
        return runCatching {
            getContext().packageManager.getPackageInfo(getContext().packageName, 0).versionName
        }.getOrNull() ?: "nameNotFound"
    }

    /**
     * Retrieves the application's name as displayed in the launcher.
     *
     * - If the application label is not available, it falls back to `nonLocalizedLabel`.
     * - If both are unavailable, it returns `"UnknownApp"`.
     *
     * @return The application name or `"UnknownApp"` if retrieval fails.
     */
    private fun getAppName(): String {
        val applicationInfo = getContext().applicationInfo
        return if (applicationInfo != null) {
            val stringId = applicationInfo.labelRes
            if (stringId == 0) {
                applicationInfo.nonLocalizedLabel?.toString() ?: "UnknownApp"
            } else {
                getContext().getString(stringId) ?: "UnknownApp"
            }
        } else {
            "UnknownApp"
        }
    }

    /**
     * Builds the client information header (X-Stream-Client) that will be added to requests.
     *
     * @return Header value as a string.
     */

    internal fun buildSdkTrackingHeaders(): String {
        return buildString {
            append("$VERSION_PREFIX_HEADER-${BuildConfig.STREAM_VIDEO_VERSION}")
            append("|os=Android ${Build.VERSION.RELEASE}")
            append("|api_version=${Build.VERSION.SDK_INT}")
            append("|device_model=${Build.MANUFACTURER} ${Build.MODEL}")
            append(buildAppVersionForHeader())
            append(buildAppName()) // Assumes buildAppName() returns a properly formatted string
        }
    }

    private fun buildAppVersionForHeader() = (StreamVideo.instance() as? StreamVideoClient)?.let { streamVideoImpl ->
        "|app_version=" + (streamVideoImpl.appVersion ?: getAppVersionName())
    } ?: ""

    private fun buildAppName(): String =
        (StreamVideo.instance() as? StreamVideoClient)?.let { streamVideoImpl ->
            "|app_name=" + (streamVideoImpl.appName ?: getAppName())
        } ?: ""

    private fun getContext(): Context {
        return StreamVideo.instance().context
    }
}
