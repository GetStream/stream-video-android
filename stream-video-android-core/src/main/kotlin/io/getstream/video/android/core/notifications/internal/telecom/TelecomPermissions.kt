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

// TODO pass StreamVideo instance in constructor on v2
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

    private fun getMissingPermissions(context: Context): List<String> =
        getRequiredPermissionsArray().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    private fun optedForTelecom() = (StreamVideo.instanceOrNull() as? StreamVideoClient)?.telecomConfig != null

    fun canUseTelecom(callServiceConfig: CallServiceConfig, context: Context): Boolean {
        if (!callServiceConfig.enableTelecom) {
            logger.d { "[canUseTelecom] Telecom is disabled by CallServiceConfig." }
            return false
        }

        if (!optedForTelecom()) {
            logger.d { "[canUseTelecom] StreamVideo was not configured with telecomConfig." }
            return false
        }

        if (!supportsTelecom(context)) {
            return false
        }

        val missingPermissions = getMissingPermissions(context)
        if (missingPermissions.isNotEmpty()) {
            logger.d {
                "[canUseTelecom] Missing required permissions: ${missingPermissions.joinToString()}"
            }
            return false
        }

        return true
    }

    fun supportsTelecom(context: Context): Boolean {
        val pm = context.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            logger.d { "[canUseTelecom] Device does not support telephony." }
            return false
        }

        val telecomManager = getSafeTelecomManager(context)
        if (telecomManager == null) {
            logger.d { "[canUseTelecom] TelecomManager is unavailable." }
            return false
        }

        if (telecomManager.defaultDialerPackage.isNullOrEmpty()) {
            logger.d { "[canUseTelecom] No default dialer is configured." }
            return false
        }

        return true
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
