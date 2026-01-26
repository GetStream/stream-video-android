/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions

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
    permissions: List<String> = getPermissions(),
    onPermissionsResult: ((Map<String, Boolean>) -> Unit)? = null,
    onAllPermissionsGranted: (suspend () -> Unit)? = null,
): VideoPermissionsState {
    if (LocalInspectionMode.current) return fakeVideoPermissionsState

    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()

    val permissionState = rememberMultiplePermissionsState(permissions) {
        if (onPermissionsResult != null) {
            onPermissionsResult.invoke(it)
        } else {
            if (it[android.Manifest.permission.CAMERA] == true && isCameraEnabled) {
                call.camera.setEnabled(true, fromUser = false)
            }
            if (it[android.Manifest.permission.RECORD_AUDIO] == true && isMicrophoneEnabled) {
                call.microphone.setEnabled(true, fromUser = false)
            }
        }
    }

    val allPermissionsGranted = permissionState.allPermissionsGranted
    LaunchedEffect(key1 = allPermissionsGranted) {
        if (allPermissionsGranted) {
            onAllPermissionsGranted?.invoke()
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

@Composable
private fun getPermissions(): List<String> {
    val context = LocalContext.current
    val permissionsList = mutableListOf<String>()
    val telecomPermissions = TelecomPermissions()

    if (telecomPermissions.supportsTelecom(context)) {
        val telecomIntegrationType = StreamVideo.instanceOrNull()?.state?.getTelecomIntegrationType()
        telecomIntegrationType?.let {
            permissionsList.addAll(
                telecomPermissions.getRequiredPermissionsArray(telecomIntegrationType),
            )
        }
    }

    permissionsList.addAll(
        mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
        ),
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionsList.addAll(
            mutableListOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ),
        )
    }
    return permissionsList
}

/**
 * Lunch call permissions about:
 *
 * - android.Manifest.permission.CAMERA
 * - android.Manifest.permission.RECORD_AUDIO
 */
@Composable
public fun LaunchCallPermissions(
    call: Call,
    onPermissionsResult: ((Map<String, Boolean>) -> Unit)? = null,
    onAllPermissionsGranted: (suspend () -> Unit)? = null,
) {
    val callPermissionsState =
        rememberCallPermissionsState(
            call = call,
            onPermissionsResult = onPermissionsResult,
            onAllPermissionsGranted = onAllPermissionsGranted,
        )
    LaunchedEffect(key1 = call) { callPermissionsState.launchPermissionRequest() }
}
