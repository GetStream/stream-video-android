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

package io.getstream.video.android.core.notifications.medianotifications

import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.annotation.DrawableRes

// 1. Content Configuration (Title and Subtitle)
data class MediaNotificationContent(
    val contentTitle: String,
    val contentText: String? = null,
)

// 2. Visual Configuration (Icons and Banner)
data class MediaNotificationVisuals(
    @DrawableRes val iconRes: Int = android.R.drawable.ic_media_play,
    val bannerBitmap: Bitmap? = null,
)

data class MediaNotificationConfig(
    val mediaNotificationContent: MediaNotificationContent,
    val mediaNotificationVisuals: MediaNotificationVisuals,
    val contentIntent: PendingIntent?,
)
