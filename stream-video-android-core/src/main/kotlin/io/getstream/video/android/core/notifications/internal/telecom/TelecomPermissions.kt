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

package io.getstream.video.android.core.notifications.internal.telecom

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig

class TelecomPermissions {

    private val logger: TaggedLogger by taggedLogger("TelecomPermissions")

    private fun getRequiredPermissionsList(): List<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            with(permissions) {
                add(android.Manifest.permission.MANAGE_OWN_CALLS)
            }
        }
        return permissions
    }

    private fun getRequiredPermissionsList(telecomIntegrationType: TelecomIntegrationType): List<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            with(permissions) {
                add(android.Manifest.permission.MANAGE_OWN_CALLS)
            }
        }
        return permissions
    }

    private fun getRequiredPermissionsArray(): Array<String> {
        return getRequiredPermissionsList().toTypedArray()
    }

    fun getRequiredPermissionsArray(telecomIntegrationType: TelecomIntegrationType): Array<String> {
        return getRequiredPermissionsList(telecomIntegrationType).toTypedArray()
    }

    private fun hasPermissions(context: Context): Boolean {
        return getRequiredPermissionsArray().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun optedForTelecom() = (StreamVideo.instanceOrNull() as? StreamVideoClient)?.telecomConfig != null

    fun canUseTelecom(callServiceConfig: CallServiceConfig, context: Context): Boolean {
        return callServiceConfig.enableTelecom && optedForTelecom() && supportsTelecom(context) && hasPermissions(context)
    }

    fun supportsTelecom(context: Context): Boolean {
        val pm = context.packageManager
        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val hasDefaultDialer = getSafeTelecomManager(context)?.defaultDialerPackage?.isNotEmpty() == true
        return hasTelephony && hasDefaultDialer
    }

    private fun getSafeTelecomManager(context: Context): TelecomManager? {
        try {
            val telecomManager = context.getSystemService(
                Context.TELECOM_SERVICE,
            ) as? TelecomManager
            return telecomManager
        } catch (e: AssertionError) {
            // Paparazzi/Robolectric throws AssertionError: Unsupported Service: telecom
            return null
        }
    }
}
