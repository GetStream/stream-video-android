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

package io.getstream.video.android.core.notifications.internal.storage

import android.app.Notification
import io.getstream.video.android.core.Call

// TODO Rahul - Delete this before merge
class DefaultNotificationDataStore : NotificationDataStore {

    /**
     * Map of a call id with Notification
     * You can get Call id via Call.id
     */
    private val notificationsMap: HashMap<String, Notification> = HashMap()

    override fun getNotification(callId: String): Notification? {
        return notificationsMap[callId]
    }

    override fun getNotification(call: Call): Notification? {
        return notificationsMap[call.id]
    }

    override fun saveNotification(callId: String, notification: Notification) {
        notificationsMap[callId] = notification
    }

    override fun clear(callId: String) {
        notificationsMap.remove(callId)
    }

    override fun clearAll() {
        notificationsMap.clear()
    }
}
