/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.app.router

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import io.getstream.logging.StreamLog
import io.getstream.video.android.app.ui.home.HomeActivity
import io.getstream.video.android.app.ui.login.LoginActivity
import io.getstream.video.android.router.StreamRouter

class StreamRouterImpl : StreamRouter, Application.ActivityLifecycleCallbacks {

    private val logger = StreamLog.getLogger("Call:StreamRouter")

    private var currentActivity: Activity? = null

    override fun finish() {
        logger.i { "[finish] no args" }
        currentActivity?.finish()
    }

    override fun onUserLoggedIn() {
        logger.i { "[onUserLoggedIn] no args" }
        currentActivity?.also {
            it.startActivity(HomeActivity.getIntent(it))
            it.finish()
        }
    }

    override fun onUserLoggedOut() {
        logger.i { "[onUserLoggedOut] no args" }
        currentActivity?.also {
            it.startActivity(Intent(it, LoginActivity::class.java))
            it.finish()
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) { currentActivity = activity }

    override fun onActivityStarted(activity: Activity) { currentActivity = activity }

    override fun onActivityResumed(activity: Activity) { currentActivity = activity }

    override fun onActivityPaused(activity: Activity) { currentActivity = null }

    override fun onActivityStopped(activity: Activity) { /* no-op */ }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) { /* no-op */ }

    override fun onActivityDestroyed(activity: Activity) { /* no-op */ }
}
