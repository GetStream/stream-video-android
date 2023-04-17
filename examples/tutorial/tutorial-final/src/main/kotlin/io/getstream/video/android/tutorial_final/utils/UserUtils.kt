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

package io.getstream.video.android.tutorial_final.utils

import io.getstream.video.android.core.model.User

fun getUsers(): List<User> {
    return listOf(
        User(
            id = "filip",
            name = "Filip",
            image = "https://avatars.githubusercontent.com/u/17215808?v=4",
            role = "admin",
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = "thierry",
            name = "Thierry",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            role = "admin",
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = "martin",
            name = "Martin",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            role = "admin",
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = "oliver",
            name = "Oliver",
            image = "https://www.biografija.org/wp-content/uploads/2020/01/boban-rajovic.jpg",
            role = "admin",
            teams = emptyList(),
            custom = emptyMap()
        ),
        User(
            id = "tomislav",
            name = "Tomislav",
            image = "https://i.scdn.co/image/ab67616d0000b2730ae491943a8668e81e212594",
            role = "admin",
            teams = emptyList(),
            custom = emptyMap()
        )
    )
}
