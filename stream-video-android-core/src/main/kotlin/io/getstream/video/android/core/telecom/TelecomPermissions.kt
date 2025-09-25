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

package io.getstream.video.android.core.telecom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.telecom.ui.TelecomPermissionRequestActivity

public class TelecomPermissions {

    private val logger: TaggedLogger by taggedLogger("StreamVideo:TelecomPermissions")

    fun getRequiredPermissionsList(): List<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            with(permissions) {
                add(android.Manifest.permission.MANAGE_OWN_CALLS)
                add(android.Manifest.permission.READ_PHONE_NUMBERS)
            }
        }
        return permissions
    }

    fun getRequiredPermissionsArray(): Array<String> {
        return getRequiredPermissionsList().toTypedArray()
    }

    fun hasPermissions(context: Context): Boolean {
        return getRequiredPermissionsArray().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    internal fun requestPermissions(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>,
    ) {
        val intent = Intent(activity, TelecomPermissionRequestActivity::class.java)
        launcher.launch(intent)
    }

    fun canUseTelecom(context: Context): Boolean {
        val hasTelecomConfig = (StreamVideo.instanceOrNull() as? StreamVideoClient)?.telecomConfig != null
        return hasTelecomConfig && supportsTelecom(context) && hasPermissions(context)
    }

    fun supportsTelecom(context: Context): Boolean {
        val pm = context.packageManager
        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val hasDefaultDialer = telecomManager?.defaultDialerPackage?.isNotEmpty() == true

        return hasTelephony && hasDefaultDialer
    }

    fun requestPermissions(activity: ComponentActivity, callback: (Boolean) -> Unit) {
        if (!supportsTelecom(activity)) {
            logger.d { "Telecom not supported on this device" }
            callback(false)
            return
        }

        if (hasPermissions(activity)) {
            callback(true)
            return
        }

        // Register a launcher on the fly
        val launcher: ActivityResultLauncher<Intent> =
            activity.activityResultRegistry.register(
                "telecom_${System.currentTimeMillis()}",
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val granted = result.resultCode == Activity.RESULT_OK &&
                    (result.data?.getBooleanExtra(TelecomPermissionRequestActivity.INTENT_EXTRA_TELECOM_PERMISSION_GRANTED, false) ?: false)
                logger.d { "Telecom Permission granted: $granted" }
                callback(granted)
            }

        val intent = Intent(activity, TelecomPermissionRequestActivity::class.java)
        launcher.launch(intent)
    }
}
