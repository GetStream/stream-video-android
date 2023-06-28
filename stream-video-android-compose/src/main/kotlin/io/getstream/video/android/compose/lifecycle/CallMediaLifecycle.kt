/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.getstream.video.android.compose.pip.enterPictureInPicture
import io.getstream.video.android.core.Call
import kotlinx.coroutines.delay

/**
 * Register a call media lifecycle that controls camera and microphone depending on lifecycles.
 * The default behavior is like so:
 *
 * - camera will be disabled if the lifecycle is onPaused, and not on the PIP mode.
 * - camera will be enabled if the lifecycle is onResumed, and not on the PIP mode.
 *
 * @param call The call includes states and will be rendered with participants.
 * @param isInPictureInPicture Whether the user has engaged in Picture-In-Picture mode.
 * @param pipEnteringDuration The duration requires to be engaged in Picture-In-Picture mode.
 */
@Composable
public fun CallMediaLifecycle(
    call: Call,
    isInPictureInPicture: Boolean,
    enableInPictureInPicture: Boolean = false,
    pipEnteringDuration: Long = 100
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var latestLifecycleEvent by remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            latestLifecycleEvent = event
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    if (latestLifecycleEvent == Lifecycle.Event.ON_PAUSE) {
        LaunchedEffect(latestLifecycleEvent) {
            delay(pipEnteringDuration)
            if (isInPictureInPicture) {
                call.camera.pause()
            } else if (enableInPictureInPicture) {
                enterPictureInPicture(context = context, call = call)
            }
        }
    }

    if (latestLifecycleEvent == Lifecycle.Event.ON_RESUME) {
        LaunchedEffect(latestLifecycleEvent) {
            delay(pipEnteringDuration)
            if (isInPictureInPicture) {
                call.camera.resume()
            }
        }
    }
}
