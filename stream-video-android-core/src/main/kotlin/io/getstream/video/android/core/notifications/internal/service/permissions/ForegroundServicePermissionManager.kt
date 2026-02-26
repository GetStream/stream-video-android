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

package io.getstream.video.android.core.notifications.internal.service.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.notifications.internal.service.CallService

internal open class ForegroundServicePermissionManager {
    @SuppressLint("InlinedApi")
    internal open val requiredForegroundTypes = setOf(
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
    )

    /**
     * Map each service type to the permission it requires (if any).
     * Subclasses can reuse or extend this mapping.
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK] requires Q
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL] requires Q
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA] requires R
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE] requires R
     * [ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE] requires UPSIDE_DOWN_CAKE
     */
    @SuppressLint("InlinedApi")
    private val typePermissionsMap = mapOf(
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA to Manifest.permission.CAMERA,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE to Manifest.permission.RECORD_AUDIO,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK to null,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL to null,
    )

    fun getServiceType(context: Context, trigger: CallService.Companion.Trigger): Int {
        return when (trigger) {
            CallService.Companion.Trigger.OnGoingCall -> calculateServiceType(context)
            else -> noPermissionServiceType()
        }
    }

    private fun calculateServiceType(context: Context): Int {
        return if (hasAllPermissions(context)) {
            allPermissionsServiceType()
        } else {
            noPermissionServiceType()
        }
    }

    @SuppressLint("InlinedApi")
    internal fun allPermissionsServiceType(): Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            requiredForegroundTypes.reduce { acc, type -> acc or type }
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
            androidQServiceType()
        else -> {
            /**
             * Android Pre-Q Service Type (no need to bother)
             * We don't start foreground service with type
             */
            0
        }
    }

    @SuppressLint("InlinedApi")
    internal open fun noPermissionServiceType(): Int = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        else ->
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
    }

    @SuppressLint("InlinedApi")
    internal open fun androidQServiceType(): Int {
        return if (requiredForegroundTypes.contains(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
            )
        ) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
        } else {
            /**
             *  Existing behavior
             *  [ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE] requires [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]
             */
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    internal fun hasAllPermissions(context: Context): Boolean {
        return requiredForegroundTypes.all { type ->
            val permission = typePermissionsMap[type]
            permission == null || ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}
