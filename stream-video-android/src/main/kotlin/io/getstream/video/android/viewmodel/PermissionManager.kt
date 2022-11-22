/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.viewmodel

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface used to keep track of permissions needed for the Video call to work.
 */
public interface PermissionManager {

    /**
     * State of the record audio permission.
     */
    public val hasRecordAudioPermission: StateFlow<Boolean>

    /**
     * State of the video permission.
     */
    public val hasCameraPermission: StateFlow<Boolean>

    /**
     * Used to check whether a permission is granted.
     *
     * @param permission The permission for which we want to check if it is granted. Use [Manifest.permission]
     *
     * @return Whether the permission is granted or not.
     */
    public fun checkPermission(permission: String): Boolean

    /**
     * Used to request a permission or returns true if already granted.
     *
     * @param permission The permission which we want to be be granted.
     *
     * @return Returns true if permission is already granted and false if the permission needs to be requested.
     */
    public fun requestPermission(permission: String): Boolean
}

/**
 * Implementation of [PermissionManager] which keeps track of app permissions and gives us the possibility to react
 * to permission results and to show settings.
 *
 * @param fragmentActivity [FragmentActivity] used to [FragmentActivity.registerForActivityResult] so we can request
 * permissions.
 * @param onPermissionResult Callback used to notify when user grants/denys a permission.
 * @param onShowSettings Callback used when the user has selected don't allow and we need to take them to the
 * settings to grant the permissions
 */
public class PermissionManagerImpl(
    private val fragmentActivity: FragmentActivity,
    private val onPermissionResult: (String, Boolean) -> Unit,
    private val onShowSettings: (String) -> Unit,
) : PermissionManager {

    /**
     * Used to request permissions. Notifies of result using [onPermissionResult].
     */
    private val permissionsContract = fragmentActivity
        .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, isGranted) ->
                when (permission) {
                    Manifest.permission.RECORD_AUDIO -> _hasRecordAudioPermission.value = isGranted
                    Manifest.permission.CAMERA -> _hasCameraPermission.value = isGranted
                }

                onPermissionResult(permission, isGranted)
            }
        }

    /**
     * State of audio permission.
     */
    private val _hasRecordAudioPermission: MutableStateFlow<Boolean> =
        MutableStateFlow(checkPermission(Manifest.permission.RECORD_AUDIO))
    override val hasRecordAudioPermission: StateFlow<Boolean> = _hasRecordAudioPermission

    /**
     * State of camera permission.
     */
    private val _hasCameraPermission: MutableStateFlow<Boolean> =
        MutableStateFlow(checkPermission(Manifest.permission.CAMERA))
    override val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission

    /**
     * Used to check whether a permission is granted.
     *
     * @param permission The permission for which we want to check if it is granted. Use [Manifest.permission]
     *
     * @return Whether the permission is granted or not.
     */
    override fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            fragmentActivity.applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Used to request a permission or returns true if already granted. If the user has already selected don't allow,
     * will notify that we need to take the user to the settings using [onShowSettings] callback.
     *
     * @param permission The permission which we want to be be granted.
     *
     * @return Returns true if permission is already granted and false if the permission needs to be requested.
     */
    override fun requestPermission(permission: String): Boolean {
        if (checkPermission(permission)) return true
        if (ActivityCompat.shouldShowRequestPermissionRationale(fragmentActivity, permission)) {
            onShowSettings(permission)
        } else {
            permissionsContract.launch(arrayOf(permission))
        }
        return false
    }
}
