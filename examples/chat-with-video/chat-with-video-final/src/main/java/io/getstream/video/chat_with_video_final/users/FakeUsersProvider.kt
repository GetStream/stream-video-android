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

package io.getstream.video.chat_with_video_final.users

import io.getstream.video.android.core.user.UsersProvider
import io.getstream.video.android.model.User
import io.getstream.video.chat_with_video_final.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUsersProvider : UsersProvider {

    override fun provideUsers(): List<io.getstream.video.android.model.User> {
        return mockUsers()
    }

    private fun mockUsers(): List<io.getstream.video.android.model.User> {
        return listOf(
            io.getstream.video.android.model.User(
                id = BuildConfig.SAMPLE_USER_00_ID,
                name = BuildConfig.SAMPLE_USER_00_NAME,
                role = BuildConfig.SAMPLE_USER_00_ROLE,
                image = BuildConfig.SAMPLE_USER_00_IMAGE,
                custom = mapOf(
                    "chatToken" to BuildConfig.SAMPLE_USER_00_CHAT_TOKEN
                ),
                teams = emptyList()
            ),
            io.getstream.video.android.model.User(
                id = BuildConfig.SAMPLE_USER_01_ID,
                name = BuildConfig.SAMPLE_USER_01_NAME,
                role = BuildConfig.SAMPLE_USER_01_ROLE,
                image = BuildConfig.SAMPLE_USER_01_IMAGE,
                custom = mapOf(
                    "chatToken" to BuildConfig.SAMPLE_USER_01_CHAT_TOKEN
                ),
                teams = emptyList()
            ),
            io.getstream.video.android.model.User(
                id = BuildConfig.SAMPLE_USER_02_ID,
                name = BuildConfig.SAMPLE_USER_02_NAME,
                role = BuildConfig.SAMPLE_USER_02_ROLE,
                image = BuildConfig.SAMPLE_USER_02_IMAGE,
                custom = mapOf(
                    "chatToken" to BuildConfig.SAMPLE_USER_02_CHAT_TOKEN
                ),
                teams = emptyList()
            ),
            io.getstream.video.android.model.User(
                id = BuildConfig.SAMPLE_USER_03_ID,
                name = BuildConfig.SAMPLE_USER_03_NAME,
                role = BuildConfig.SAMPLE_USER_03_ROLE,
                image = BuildConfig.SAMPLE_USER_03_IMAGE,
                custom = mapOf(
                    "chatToken" to BuildConfig.SAMPLE_USER_03_CHAT_TOKEN
                ),
                teams = emptyList()
            ),
            io.getstream.video.android.model.User(
                id = BuildConfig.SAMPLE_USER_04_ID,
                name = BuildConfig.SAMPLE_USER_04_NAME,
                role = BuildConfig.SAMPLE_USER_04_ROLE,
                image = BuildConfig.SAMPLE_USER_04_IMAGE,
                custom = mapOf(
                    "chatToken" to BuildConfig.SAMPLE_USER_04_CHAT_TOKEN
                ),
                teams = emptyList()
            ),
            io.getstream.video.android.model.User(
                id = BuildConfig.SAMPLE_USER_05_ID,
                name = BuildConfig.SAMPLE_USER_05_NAME,
                role = BuildConfig.SAMPLE_USER_05_ROLE,
                image = BuildConfig.SAMPLE_USER_05_IMAGE,
                custom = mapOf(
                    "chatToken" to BuildConfig.SAMPLE_USER_05_CHAT_TOKEN
                ),
                teams = emptyList()
            ),
        )
    }

    override val userState: StateFlow<List<io.getstream.video.android.model.User>> = MutableStateFlow(provideUsers())
}
