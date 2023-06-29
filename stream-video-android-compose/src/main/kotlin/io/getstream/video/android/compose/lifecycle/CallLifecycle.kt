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
import io.getstream.video.android.compose.pip.isInPictureInPictureMode
import io.getstream.video.android.core.Call
import kotlinx.coroutines.delay

/**
 * Register a call lifecycle that leaves a call depending on lifecycles.
 * The default behavior is like so:
 *
 * - call will be leaved if the lifecycle is onDestroyed.
 * - call will be leaved if the lifecycle is onStop, and on the PIP mode.
 *
 * @param call The call includes states and will be rendered with participants.
 * @param pipEnteringDuration The duration requires to be engaged in Picture-In-Picture mode.
 */
@Composable
public fun CallLifecycle(
    call: Call,
    enableInPictureInPicture: Boolean = false,
    pipEnteringDuration: Long = 250
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

    if (latestLifecycleEvent == Lifecycle.Event.ON_DESTROY) {
        LaunchedEffect(latestLifecycleEvent) {
            call.leave()
        }
    }

    if (latestLifecycleEvent == Lifecycle.Event.ON_STOP) {
        LaunchedEffect(latestLifecycleEvent) {
            delay(pipEnteringDuration)
            val isInPictureInPicture = context.isInPictureInPictureMode
            if (isInPictureInPicture && enableInPictureInPicture) {
                call.leave()
            }
        }
    }
}
