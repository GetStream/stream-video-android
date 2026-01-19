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

package io.getstream.video.android.ui.common.permission

public interface PermissionManagerProvider {

    /**
     * Used for [PermissionManager] setup and registration with the Activity that it's used in.
     *
     * @return The initialized and registered [PermissionManager].
     */
    public fun initPermissionManager(): PermissionManager

    /**
     * Allows fetching of the [PermissionManager] instance created by [initPermissionManager].
     *
     * @return The initialized [PermissionManager] instance.
     */
    public fun getPermissionManager(): PermissionManager
}
