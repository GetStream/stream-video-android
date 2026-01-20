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

import androidx.compose.runtime.Stable

/**
 * Permission state interface that allows you to request Android permissions or check if those
 * permissions are granted or not.
 */
@Stable
public interface VideoPermissionsState {

    /**
     * When `true`, the user has granted all permissions.
     */
    public val allPermissionsGranted: Boolean

    /**
     * When `true`, the user should be presented with a rationale.
     */
    public val shouldShowRationale: Boolean

    /**
     * Request the permissions to the user.
     *
     * This should always be triggered from non-composable scope, for example, from a side-effect
     * or a non-composable callback. Otherwise, this will result in an IllegalStateException.
     *
     * This triggers a system dialog that asks the user to grant or revoke the permission.
     * Note that this dialog might not appear on the screen if the user doesn't want to be asked
     * again or has denied the permission multiple times.
     * This behavior varies depending on the Android level API.
     */
    public fun launchPermissionRequest(): Unit
}

internal val fakeVideoPermissionsState = object : VideoPermissionsState {
    override val allPermissionsGranted: Boolean = false
    override val shouldShowRationale: Boolean = false
    override fun launchPermissionRequest() = Unit
}
