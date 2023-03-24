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

package io.getstream.video.android.tooling.handler

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Process
import io.getstream.video.android.tooling.ui.ExceptionTraceActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

internal class StreamGlobalExceptionHandler constructor(
    application: Application,
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler,
) : Thread.UncaughtExceptionHandler {

    private var lastActivity: Activity? = null
    private var activityCount = 0

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {

                override fun onActivityResumed(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) =
                    Unit

                override fun onActivityDestroyed(activity: Activity) = Unit

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (isExceptionActivity(activity)) {
                        return
                    }
                    lastActivity = activity
                }

                override fun onActivityStarted(activity: Activity) {
                    if (isExceptionActivity(activity)) {
                        return
                    }
                    activityCount++
                    lastActivity = activity
                }

                override fun onActivityStopped(activity: Activity) {
                    if (isExceptionActivity(activity)) {
                        return
                    }
                    activityCount--
                    if (activityCount < 0) {
                        lastActivity = null
                    }
                }
            },
        )
    }

    private fun isExceptionActivity(activity: Activity) = activity is ExceptionTraceActivity

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        lastActivity?.run {
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))

            val stackTrace = stringWriter.toString()
            startExceptionActivity(this, stackTrace, throwable.message ?: "")
        } ?: defaultExceptionHandler.uncaughtException(thread, throwable)

        Process.killProcess(Process.myPid())
        exitProcess(-1)
    }

    private fun startExceptionActivity(activity: Activity, exception: String, message: String) =
        activity.run {
            val intent = ExceptionTraceActivity.getIntent(this, exception, message)
            startActivity(intent)
            finish()
        }
}
