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

package io.getstream.video.android.compose.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.getstream.video.android.core.Call

/**
 * Remember call related Android permissions below:
 *
 * - android.Manifest.permission.CAMERA
 * - android.Manifest.permission.RECORD_AUDIO
 *
 * You can request those permissions by invoking `launchPermissionRequest()` method.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
public fun rememberCallPermissionsState(
    call: Call,
    permissions: List<String> = mutableListOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
    ),
    onPermissionsResult: ((Map<String, Boolean>) -> Unit)? = null
): VideoPermissionsState {

    if (LocalInspectionMode.current) return fakeVideoPermissionsState

    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()

    val permissionState = rememberMultiplePermissionsState(permissions) {
        if (onPermissionsResult != null) {
            onPermissionsResult.invoke(it)
        } else {
            if (it[android.Manifest.permission.CAMERA] == true && isCameraEnabled) {
                call.camera.setEnabled(true)
            }
            if (it[android.Manifest.permission.RECORD_AUDIO] == true && isMicrophoneEnabled) {
                call.microphone.setEnabled(true)
            }
        }
    }
    return remember(call, permissions) {
        object : VideoPermissionsState {
            override val allPermissionsGranted: Boolean
                get() = permissionState.allPermissionsGranted
            override val shouldShowRationale: Boolean
                get() = permissionState.shouldShowRationale

            override fun launchPermissionRequest() {
                permissionState.launchMultiplePermissionRequest()
            }
        }
    }
}

/**
 * Lunch call permissions about:
 *
 * - android.Manifest.permission.CAMERA
 * - android.Manifest.permission.RECORD_AUDIO
 */
@Composable
public fun LaunchCallPermissions(call: Call) {
    val callPermissionsState = rememberCallPermissionsState(call = call)
    LaunchedEffect(key1 = call) { callPermissionsState.launchPermissionRequest() }
}
