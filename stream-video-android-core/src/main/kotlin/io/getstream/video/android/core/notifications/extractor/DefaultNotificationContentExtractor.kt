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

package io.getstream.video.android.core.notifications.extractor

import android.app.Notification

internal object DefaultNotificationContentExtractor : NotificationContentExtractor {

    override fun getTitle(notification: Notification): CharSequence? {
        return notification.extras.getCharSequence(Notification.EXTRA_TITLE)
    }

    override fun getText(notification: Notification): CharSequence? {
        return notification.extras.getCharSequence(Notification.EXTRA_TEXT)
    }

    override fun getSubText(notification: Notification): CharSequence? {
        return notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
    }
}
