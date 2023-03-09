/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.model

import org.openapitools.client.models.UpdateUserPermissionsRequest

/**
 * Represents the data used to request
 */
public data class UpdateUserPermissionsData(
    public val userId: String,
    public val grantedPermissions: List<String>? = null,
    public val revokedPermissions: List<String>? = null,
)

/**
 * Maps the data to the request for the BE.
 */
public fun UpdateUserPermissionsData.toRequest(): UpdateUserPermissionsRequest {
    return UpdateUserPermissionsRequest(
        userId = userId,
        grantPermissions = grantedPermissions,
        revokePermissions = revokedPermissions
    )
}
