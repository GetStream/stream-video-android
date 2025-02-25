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

package io.getstream.video.android.xml.initializer

import android.content.Context
import androidx.startup.Initializer
import io.getstream.video.android.core.header.SdkTrackingHeaders
import io.getstream.video.android.core.header.VersionPrefixHeader
import io.getstream.video.android.xml.VideoUI

/**
 * Jetpack Startup Initializer for Stream's Video UI Components.
 */
public class VideoUIInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        VideoUI.appContext = context
        SdkTrackingHeaders.VERSION_PREFIX_HEADER = VersionPrefixHeader.UiComponents
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
