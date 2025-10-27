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

package io.getstream.video.android.compose.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import io.getstream.log.StreamLog
import io.getstream.video.android.compose.pip.enterPictureInPicture
import io.getstream.video.android.compose.pip.findActivity
import io.getstream.video.android.compose.pip.isInPictureInPictureMode
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.pip.PictureInPictureConfiguration

/**
 * Register a call media lifecycle that controls camera and microphone depending on lifecycles.
 * The default behavior is like so:
 *
 * - camera/microphone will be disabled if the lifecycle is onPaused, and not on the PIP mode.
 * - camera/microphone will be enabled if the lifecycle is onResumed, and not on the PIP mode.
 *
 * @param call The call includes states and will be rendered with participants.
 */
@Composable
public fun MediaPiPLifecycle(
    call: Call,
    pictureInPictureConfiguration: PictureInPictureConfiguration =
        PictureInPictureConfiguration(true),
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val currentActivity: Activity? = LocalContext.current.findActivity()

    DisposableEffect(key1 = lifecycleOwner) {
        // TODO: There's not way to onUserLeaveHint in Compose for now. The only thing
        // that works so far is to listen to ActivityLifecycleCallbacks and enable PiP in
        // onPause.
        // https://developer.android.com/reference/android/app/Activity#onUserLeaveHint()
        val application = (context.applicationContext as? Application)
        val callbackListener = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }

            override fun onActivityStarted(activity: Activity) { }

            override fun onActivityResumed(activity: Activity) {
                if (activity == currentActivity) {
                    val isInPictureInPicture = context.isInPictureInPictureMode
                    if (!isInPictureInPicture && !pictureInPictureConfiguration.enable) {
                        call.camera.resume(fromUser = false)
                        call.microphone.resume(fromUser = false)
                    }
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity == currentActivity) {
                    val isInPictureInPicture = context.isInPictureInPictureMode
                    if (!isInPictureInPicture && !pictureInPictureConfiguration.enable) {
                        call.camera.pause(fromUser = false)
                        call.microphone.pause(fromUser = false)
                    } else if (!isInPictureInPicture) {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                enterPictureInPicture(
                                    context = context,
                                    call = call,
                                    pictureInPictureConfiguration,
                                )
                            } catch (e: Exception) {
                                StreamLog.d("MediaPiPLifecycle") { e.stackTraceToString() }
                            }
                        }
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) { }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }

            override fun onActivityDestroyed(activity: Activity) { }
        }

        application?.registerActivityLifecycleCallbacks(callbackListener)

        onDispose {
            application?.unregisterActivityLifecycleCallbacks(callbackListener)
        }
    }
}

@Deprecated(
    "Use MediaPiPLifecycle with pictureInPictureConfiguration",
    ReplaceWith("MediaPiPLifecycle(call, pictureInPictureConfiguration"),
)
@Composable
public fun MediaPiPLifecycle(
    call: Call,
    enableInPictureInPicture: Boolean = false,
) {
    MediaPiPLifecycle(
        call,
        PictureInPictureConfiguration(enableInPictureInPicture),
    )
}
