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
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.getstream.video.android.core.Call

/**
 * Remember [VideoPermissionsState] about the camera permission.
 */
@Composable
public fun rememberCameraPermissionState(
    call: Call,
    onPermissionsResult: (Boolean) -> Unit = { isGranted ->
        if (isGranted) {
            call.camera.setEnabled(true, fromUser = false)
        }
    },
): VideoPermissionsState {
    return rememberCallPermissionsState(
        call = call,
        permissions = listOf(
            android.Manifest.permission.CAMERA,
        ),
        onPermissionsResult = {
            val isGranted = it[android.Manifest.permission.CAMERA] == true
            onPermissionsResult.invoke(isGranted)
        },
    )
}

/**
 * Lunch call permissions about:
 *
 * - android.Manifest.permission.CAMERA
 */
@Composable
public fun LaunchCameraPermissions(
    call: Call,
    onPermissionsResult: (Boolean) -> Unit,
) {
    val callPermissionsState =
        rememberCameraPermissionState(call = call, onPermissionsResult = onPermissionsResult)
    LaunchedEffect(key1 = call) { callPermissionsState.launchPermissionRequest() }
}

/**
 * Remember [VideoPermissionsState] about the microphone permission.
 */
@Composable
public fun rememberMicrophonePermissionState(
    call: Call,
    onPermissionsResult: (Boolean) -> Unit = { isGranted ->
        if (isGranted) {
            call.microphone.setEnabled(true, fromUser = false)
        }
    },
): VideoPermissionsState {
    return rememberCallPermissionsState(
        call = call,
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
        ),
        onPermissionsResult = {
            val isGranted = it[android.Manifest.permission.RECORD_AUDIO] == true
            onPermissionsResult.invoke(isGranted)
        },
    )
}

/**
 * Lunch call permissions about:
 *
 * - android.Manifest.permission.RECORD_AUDIO
 */
@Composable
public fun LaunchMicrophonePermissions(
    call: Call,
    onPermissionsResult: (Boolean) -> Unit,
) {
    val callPermissionsState =
        rememberMicrophonePermissionState(call = call, onPermissionsResult = onPermissionsResult)
    LaunchedEffect(key1 = call) { callPermissionsState.launchPermissionRequest() }
}

/**
 * Remember [VideoPermissionsState] about the bluetooth permission.
 */
@Composable
@RequiresApi(Build.VERSION_CODES.S)
public fun rememberBluetoothPermissionState(
    call: Call,
    onPermissionsResult: (Boolean) -> Unit,
): VideoPermissionsState {
    return rememberCallPermissionsState(
        call = call,
        permissions = listOf(android.Manifest.permission.BLUETOOTH_CONNECT),
        onPermissionsResult = {
            val isGranted = it[android.Manifest.permission.BLUETOOTH_CONNECT] == true
            onPermissionsResult.invoke(isGranted)
        },
    )
}
