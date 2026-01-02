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

package io.getstream.video.android.datastore.model

import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import kotlinx.serialization.Serializable

/**
 * Stream login user data that is used to be stored in [StreamUserDataStore].
 *
 * @property user [User] information that is used to build a `StreamVideo` instance for logging in.
 * @property apiKey [ApiKey] information that is used to build a `StreamVideo` instance for logging in.
 * @property userToken [UserToken] information that is used to build a `StreamVideo` instance for logging in.
 * @property userDevice [Device] information that is used to be get push notifications from the Stream server.
 */
@Serializable
public data class StreamUserPreferences(
    public val user: User? = null,
    public val apiKey: ApiKey = "",
    public val userToken: UserToken = "",
    public val userDevice: Device? = null,
)
