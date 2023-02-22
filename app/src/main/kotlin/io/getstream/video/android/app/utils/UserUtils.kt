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

import io.getstream.video.android.core.model.User

fun getUsers(): List<User> {
    return listOf(
        User(
            id = "filip",
            name = "Filip",
            imageUrl = "https://avatars.githubusercontent.com/u/17215808?v=4",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZmlsaXAifQ.NBYt9PdNrTnFFl5u2xVhZ93CCMSdM7uog-DNtb8DFAA",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "thierry",
            name = "Thierry",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9.81Nhgjdnh7hnvpgqOXlGMRWkuUgCVbU-fp6gFtHymxA",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "martin",
            name = "Martin",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWFydGluIn0._oishDWVBDRRKt9VXY9RIR3Z7NunOeJkyE7mjApTux4",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "oliver",
            name = "Oliver",
            imageUrl = "https://www.biografija.org/wp-content/uploads/2020/01/boban-rajovic.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoib2xpdmVyIn0.edUI1bWxIxpPGJZeYa0k6HD58hk2FDg2Pmr2280RSGg",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "tomislav",
            name = "Tomislav",
            imageUrl = "https://i.scdn.co/image/ab67616d0000b2730ae491943a8668e81e212594",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidG9taXNsYXYifQ.aOxbaAr_UVSdpcDPJHwGwTBjDw_n4eGyHtojUUEVUt8",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        )
    )
}
