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

package io.getstream.video.android.core.telecom.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle

class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var activityCount: Int = 0

    override fun onActivityCreated(
        activity: Activity,
        bunlde: Bundle?,
    ) { // no-op
    }

    override fun onActivityStarted(activity: Activity) {
        if (activityCount++ == 0) {
            onFirstActivityStarted(activity)
        }
    }

    public open fun onFirstActivityStarted(activity: Activity) { // no-op
    }

    override fun onActivityResumed(activity: Activity) { // no-op
    }

    override fun onActivityPaused(activity: Activity) { // no-op
    }

    override fun onActivityStopped(activity: Activity) {
        if (--activityCount == 0) {
            onLastActivityStopped(activity)
        }
    }

    public open fun onLastActivityStopped(activity: Activity) { // no-op
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        bunlde: Bundle,
    ) { // no-op
    }

    override fun onActivityDestroyed(activity: Activity) { // no-op
    }
}
