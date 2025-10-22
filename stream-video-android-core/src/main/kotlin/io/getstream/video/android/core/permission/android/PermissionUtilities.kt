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

package io.getstream.video.android.core.permission.android

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.Call

/**
 * Default mapper for stream calls.
 */
internal val defaultPermissionMapper: (OwnCapability) -> String? = { capability ->
    when (capability) {
        is OwnCapability.SendAudio -> {
            android.Manifest.permission.RECORD_AUDIO
        }

        is OwnCapability.SendVideo -> {
            android.Manifest.permission.CAMERA
        }

        else -> {
            // Do not add any permission
            null
        }
    }
}

/**
 * Default expectation for all permissions that we may inquire is granted.
 */
internal val defaultPermissionExpectation: (permission: String) -> Int =
    { _ -> PackageManager.PERMISSION_GRANTED }

/**
 * Default check against the system if the permission is granted.
 */
internal fun defaultPermissionSystemCheck(context: Context): (permission: String) -> Int =
    { permission -> ContextCompat.checkSelfPermission(context, permission) }

/**
 * Maps a list of [OwnCapability] to list of [String] where the strings, are android manifest permissions.
 * used the [defaultPermissionMapper].
 *
 * Additional mapper can be supplied for different mapping.
 *
 * @return the list of permissions, based on the [OwnCapability] list.
 */
internal fun List<OwnCapability>.mapPermissions(mapper: (OwnCapability) -> String?): List<String> =
    this.mapNotNull {
        mapper.invoke(it)
    }.toSet().toList() // Remove duplicates.

/**
 * Check permission against the system.
 *
 * @param systemPermissionCheck usually equal to { ContextCompat.checkSelfPermission(context,string) }.
 * @param permissionExpectation =
 */
internal fun List<String>.checkAllPermissions(
    systemPermissionCheck: (String) -> Int,
    permissionExpectation: (String) -> Int = defaultPermissionExpectation,
): Pair<Boolean, Set<String>> {
    val permissionMissingSet = hashSetOf<String>()
    for (permission in this) {
        if (systemPermissionCheck.invoke(permission) != permissionExpectation.invoke(permission)) {
            Log.e(TAG, "permission check failed for $permission")
            permissionMissingSet.add(permission)
        }
    }
    // If all permission are according to expectation function, assume true.
    if (permissionMissingSet.isNotEmpty()) {
        return Pair(false, permissionMissingSet)
    }
    return Pair(true, emptySet())
}

/**
 * Check android permissions. for a call
 *
 * @param context the android contex.t
 * @param call the [Call].
 * @param permissionMapper mapper used to map list of [OwnCapability] into android.Manifest.* permissions.
 * @param permissionExpectation an expectation for each mapped permission one of [PackageManager.PERMISSION_GRANTED] or [PackageManager.PERMISSION_DENIED]
 */
internal fun checkPermissionsExpectations(
    context: Context,
    call: Call,
    permissionMapper: (OwnCapability) -> String? = defaultPermissionMapper,
    permissionExpectation: (String) -> Int = defaultPermissionExpectation,
): Pair<Boolean, Set<String>> {
    val a = call.state.ownCapabilities.value.mapPermissions(permissionMapper)
        .checkAllPermissions(defaultPermissionSystemCheck(context), permissionExpectation)
    return a
}

const val TAG = "PermissionUtilities"
