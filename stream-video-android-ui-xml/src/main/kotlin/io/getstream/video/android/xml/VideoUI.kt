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

package io.getstream.video.android.xml

import android.content.Context
import io.getstream.video.android.xml.font.VideoFonts
import io.getstream.video.android.xml.font.VideoFontsImpl
import io.getstream.video.android.xml.font.VideoStyle
import io.getstream.video.android.xml.utils.lazyVar

/**
 * VideoUI handles any configuration for the Video UI elements.
 *
 * @see VideoFonts
 */
public object VideoUI {
    internal lateinit var appContext: Context

    @JvmStatic
    public var style: VideoStyle = VideoStyle()

    /**
     * Allows setting default fonts used by UI components.
     */
    @JvmStatic
    public var fonts: VideoFonts by lazyVar { VideoFontsImpl(style, appContext) }
}
