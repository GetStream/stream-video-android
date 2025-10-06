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

package io.getstream.video.android.core.notifications.internal.telecom.ui

import android.app.Activity
import android.app.Application
import android.widget.Toast
import androidx.activity.ComponentActivity
import io.getstream.android.push.permissions.ActivityLifecycleCallbacks
import io.getstream.android.push.permissions.R
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions

class TelecomPermissionHandler private constructor() : ActivityLifecycleCallbacks() {
    private val logger by taggedLogger("TelecomPermissionHandler")
    private val telecomPermission = TelecomPermissions()
    private var currentActivity: Activity? = null

    override fun onActivityStarted(activity: Activity) {
        super.onActivityStarted(activity)
        currentActivity = activity
    }

    override fun onLastActivityStopped(activity: Activity) {
        super.onLastActivityStopped(activity)
        currentActivity = null
    }

    override fun onFirstActivityStarted(activity: Activity) {
        super.onFirstActivityStarted(activity)
        if (activity is ComponentActivity) {
            telecomPermission.requestPermissions(activity) { granted -> }
        }
    }

//    override fun onPermissionRequested() { // no-op
//    }
//
//    override fun onPermissionGranted() { // no-op
//    }
//
//    override fun onPermissionDenied() {
//        logger.i { "[onPermissionDenied] currentActivity: $currentActivity" }
//        currentActivity?.showNotificationBlocked()
//    }
//
//    override fun onPermissionRationale() { // no-op
//    }

    private fun Activity.showNotificationBlocked() {
        Toast.makeText(
            this,
            R.string.stream_push_permissions_notifications_message,
            Toast.LENGTH_LONG,
        ).show()
    }

    public companion object {
        public fun instance(
            application: Application,
        ): TelecomPermissionHandler =
            TelecomPermissionHandler()
                .also { application.registerActivityLifecycleCallbacks(it) }
    }
}
