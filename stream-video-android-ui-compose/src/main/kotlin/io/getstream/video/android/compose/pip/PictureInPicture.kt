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

package io.getstream.video.android.compose.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import io.getstream.video.android.core.Call

@Suppress("DEPRECATION")
internal fun enterPictureInPicture(context: Context, call: Call) {
    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentOrientation = context.resources.configuration.orientation
            val screenSharing = call.state.screenSharingSession.value

            val aspect =
                if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && (screenSharing == null || screenSharing.participant.isLocal)) {
                    Rational(9, 16)
                } else {
                    Rational(16, 9)
                }

            val params = PictureInPictureParams.Builder()
            params.setAspectRatio(aspect).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setTitle("Video Player")
                    setSeamlessResizeEnabled(true)
                }
            }

            context.findActivity()?.enterPictureInPictureMode(params.build())
        } else {
            context.findActivity()?.enterPictureInPictureMode()
        }
    }
}

/**
 * Remember if the current activity is in Picture-in-Picture mode.
 * To be used in compose to  decide weather the current mode is PiP or not.
 */
@Composable
public fun rememberIsInPipMode(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val activity = LocalContext.current.findComponentActivity()
        var pipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode) }
        DisposableEffect(activity) {
            val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
            activity?.addOnPictureInPictureModeChangedListener(
                observer,
            )
            onDispose { activity?.removeOnPictureInPictureModeChangedListener(observer) }
        }
        return pipMode ?: false
    } else {
        return false
    }
}

/**
 * Used in other parts of the app to check if the current context is in Picture-in-Picture mode.
 */
public val Context.isInPictureInPictureMode: Boolean
    get() {
        val currentActivity = findActivity()
        return currentActivity?.isInPictureInPictureMode == true
    }

internal fun Context.findComponentActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
