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

package io.getstream.video.android.tutorial.ringing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


fun Context.isAudioPermissionGranted() = ContextCompat.checkSelfPermission(
    this, Manifest.permission.RECORD_AUDIO,
) == PackageManager.PERMISSION_GRANTED

fun Context.isCameraPermissionGranted() = ContextCompat.checkSelfPermission(
    this, Manifest.permission.CAMERA,
) == PackageManager.PERMISSION_GRANTED

fun ComponentActivity.defaultPermissionLauncher(allGranted: () -> Unit = {}) = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
) { granted ->
    // Handle the permissions result here
    granted.entries.forEach { (permission, granted) ->
        if (!granted) {
            Toast.makeText(this, "$permission permission is required.", Toast.LENGTH_LONG).show()
        }
    }
    if (granted.entries.all { it.value }) {
        allGranted()
    }
}

fun ActivityResultLauncher<Array<String>>.requestDefaultPermissions() {
    var permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    launch(permissions)
}