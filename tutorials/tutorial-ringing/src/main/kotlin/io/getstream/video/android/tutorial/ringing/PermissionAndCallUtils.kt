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
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.notifications.NotificationHandler

/**
 * Request the audio permission for a result launcher.
 */
fun ActivityResultLauncher<String>.requestAudioPermission() =
    launch(Manifest.permission.RECORD_AUDIO)

/**
 * Check if RECORD_AUDIO is granted.
 */
fun Context.isAudioPermissionGranted() = ContextCompat.checkSelfPermission(
    this, Manifest.permission.RECORD_AUDIO,
) == PackageManager.PERMISSION_GRANTED

/**
 * Check if the current activity was started as a caller.
 */
fun ComponentActivity.isCaller() = intent.action == NotificationHandler.ACTION_OUTGOING_CALL
