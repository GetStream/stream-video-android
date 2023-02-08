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

package io.getstream.video.android.tutorial_starter.user

import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UsersProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUsersProvider : UsersProvider {

    override fun provideUsers(): List<User> {
        return mockUsers()
    }

    private fun mockUsers(): List<User> {
        return listOf(
            User(
                id = "vasil",
                name = "Vasil Valkanov",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/VASIL_VALKANOV.jpg",
                token = "",
                extraData = emptyMap(),
                teams = emptyList()
            ),
            User(
                id = "veselin",
                name = "Veselin Marinov",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/Veselin_Marinov_2021-08-04.jpg",
                token = "",
                extraData = emptyMap(),
                teams = emptyList()
            ),
            User(
                id = "valia",
                name = "Valia",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/Valia_-_Site_April.jpg",
                token = "",
                extraData = emptyMap(),
                teams = emptyList()
            ),
            User(
                id = "damjan",
                name = "Damjan Popov",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/DAMYAN_POPOV.jpg",
                token = "",
                extraData = emptyMap(),
                teams = emptyList()
            ),
            User(
                id = "jordan",
                name = "Jordan",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/DJORDAN_-_SEPT_-_2022.jpg",
                token = "",
                extraData = emptyMap(),
                teams = emptyList()
            ),
            User(
                id = "ina",
                name = "Ina Garjadi",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/INA_GAYARDI_-_PAYNER_-_OCTOBER_-_2022.jpg",
                token = "",
                extraData = emptyMap(),
                teams = emptyList()
            ),
        )
    }

    override val userState: StateFlow<List<User>> = MutableStateFlow(provideUsers())
}
