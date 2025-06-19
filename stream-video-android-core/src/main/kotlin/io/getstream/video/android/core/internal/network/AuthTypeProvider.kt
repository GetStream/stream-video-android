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

package io.getstream.video.android.core.internal.network

internal class AuthTypeProvider {
    private var authType: AuthType = AuthType.JWT

    fun getAuthType(): String {
        return authType.value
    }

    fun setAuthType(authType: AuthType) {
        this.authType = authType
    }

    internal enum class AuthType(val value: String) {
        JWT("jwt"),
        ANONYMOUS("anonymous"),
    }
}
