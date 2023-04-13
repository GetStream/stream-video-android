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

package io.getstream.video.android.app.utils

import io.getstream.video.android.app.BuildConfig
import io.getstream.video.android.core.model.User

fun getUsers(): List<User> {
    // TODO: using buildconfig for this is not ideal
    return listOf(
        User(
            id = BuildConfig.SAMPLE_USER_00_ID,
            name = BuildConfig.SAMPLE_USER_00_NAME,
            imageUrl = BuildConfig.SAMPLE_USER_00_IMAGE,
            role = BuildConfig.SAMPLE_USER_00_ROLE,
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = BuildConfig.SAMPLE_USER_01_ID,
            name = BuildConfig.SAMPLE_USER_01_NAME,
            imageUrl = BuildConfig.SAMPLE_USER_01_IMAGE,
            role = BuildConfig.SAMPLE_USER_01_ROLE,
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = BuildConfig.SAMPLE_USER_02_ID,
            name = BuildConfig.SAMPLE_USER_02_NAME,
            imageUrl = BuildConfig.SAMPLE_USER_02_IMAGE,
            role = BuildConfig.SAMPLE_USER_02_ROLE,
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = BuildConfig.SAMPLE_USER_03_ID,
            name = BuildConfig.SAMPLE_USER_03_NAME,
            imageUrl = BuildConfig.SAMPLE_USER_03_IMAGE,
            role = BuildConfig.SAMPLE_USER_03_ROLE,
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = BuildConfig.SAMPLE_USER_04_ID,
            name = BuildConfig.SAMPLE_USER_04_NAME,
            imageUrl = BuildConfig.SAMPLE_USER_04_IMAGE,
            role = BuildConfig.SAMPLE_USER_04_ROLE,
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = BuildConfig.SAMPLE_USER_05_ID,
            name = BuildConfig.SAMPLE_USER_05_NAME,
            imageUrl = BuildConfig.SAMPLE_USER_05_IMAGE,
            role = BuildConfig.SAMPLE_USER_05_ROLE,
            teams = emptyList(),
            custom = emptyMap()
        )
    )
}
