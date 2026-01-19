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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * An API to ensure that the list of [permissions] is granted.
 *
 * Usage
 * ```kotlin
 * LaunchPermissionRequest(listOf(android.Manifest.RECORD_AUDIO, android.Manifest.CAMERA) {
 *      Granted {
 *          // All permissions granted
 *      }
 *      SomeGranted { granted, notGranted, showRationale ->
 *          // Some of the permissions were granted, you can check which ones.
 *      }
 *      NotGranted {
 *          // None of the permissions were granted.
 *      }
 * }
 *
 *
 * ```
 *
 * @param permissions the required permissions.
 * @param content the composable content used to register the
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class)
@Composable
public fun LaunchPermissionRequest(
    permissions: List<String>,
    content: @Composable LaunchPermissionRequestScope.() -> Unit,
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = permissions,
    )
    // Init scope
    val ensurePermissionScope = LaunchPermissionRequestScopeImpl()
    // Register callbacks
    content(ensurePermissionScope)

    if (permissionsState.allPermissionsGranted) {
        // All permissions are granted, call the "granted" content
        ensurePermissionScope.grantedContent()
    } else {
        val anyPermissionWasGranted = permissionsState.permissions.any {
            it.status == PermissionStatus.Granted
        }
        if (anyPermissionWasGranted) {
            val granted = permissionsState.permissions.mapNotNull {
                if (it.status == PermissionStatus.Granted) {
                    it.permission
                } else {
                    null
                }
            }
            val notGranted = permissionsState.permissions.mapNotNull {
                if (it.status is PermissionStatus.Denied) {
                    it.permission
                } else {
                    null
                }
            }

            ensurePermissionScope.someGrantedContent(
                granted,
                notGranted,
                permissionsState.shouldShowRationale,
            )
        } else {
            ensurePermissionScope.notGrantedContent(permissionsState.shouldShowRationale)
        }
    }
    LaunchedEffect(key1 = true) {
        permissionsState.launchMultiplePermissionRequest()
    }
}

/**
 * Scope for the [LaunchPermissionRequest] composable.
 * Used to register the content callbacks for when the permissions are available or not.
 */
public interface LaunchPermissionRequestScope {
    /**
     * Called after the request, when the user has granted all the requested permissions for this scope.
     */
    @Composable
    public fun AllPermissionsGranted(content: @Composable () -> Unit)

    /**
     * Some permissions were granted, while others were not.
     * Use the [content] parameters to decide what to do in certain cases if some permissions are optional to you.
     */
    @Composable
    public fun SomeGranted(
        content: @Composable (
            granted: List<String>,
            notGranted: List<String>,
            showRationale: Boolean,
        ) -> Unit,
    )

    /**
     * None of the permissions were granted.
     */
    @Composable
    public fun NoneGranted(content: @Composable (showRationale: Boolean) -> Unit)
}

private class LaunchPermissionRequestScopeImpl : LaunchPermissionRequestScope {

    var grantedContent: @Composable () -> Unit = { }
    var someGrantedContent: @Composable (
        granted: List<String>,
        notGranted: List<String>,
        showRationale: Boolean,
    ) -> Unit =
        { _, _, _ -> }
    var notGrantedContent: @Composable (showRationale: Boolean) -> Unit = { }

    @Composable
    override fun AllPermissionsGranted(content: @Composable () -> Unit) {
        grantedContent = content
    }

    @Composable
    override fun SomeGranted(
        content: @Composable (
            granted: List<String>,
            notGranted: List<String>,
            showRationale: Boolean,
        ) -> Unit,
    ) {
        someGrantedContent = content
    }

    @Composable
    override fun NoneGranted(content: @Composable (showRationale: Boolean) -> Unit) {
        notGrantedContent = content
    }
}
