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

package io.getstream.video.android.core.telecom.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import io.getstream.video.android.core.telecom.TelecomPermissions

internal class TelecomPermissionRequestActivity : Activity() {
    private val REQUEST_CODE_TELECOM = 1001

    companion object {
        const val INTENT_EXTRA_TELECOM_PERMISSION_GRANTED = "telecom_permission_granted"
    }

    private val telecomPermissions = TelecomPermissions()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (telecomPermissions.hasPermissions(this)) {
            finishWithResult(true)
        } else {
            ActivityCompat.requestPermissions(
                this,
                telecomPermissions.getRequiredPermissionsArray(),
                REQUEST_CODE_TELECOM,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_TELECOM) {
            val granted = grantResults.isNotEmpty() && grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }
            finishWithResult(granted)
        }
    }

    private fun finishWithResult(granted: Boolean) {
        val resultIntent = Intent().apply {
            putExtra(INTENT_EXTRA_TELECOM_PERMISSION_GRANTED, granted)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
