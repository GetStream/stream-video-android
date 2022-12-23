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

import io.getstream.video.android.model.User

fun getUsers(): List<User> {
    return listOf(
        User(
            id = "filip",
            name = "Filip",
            imageUrl = "https://avatars.githubusercontent.com/u/17215808?v=4",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9maWxpcCIsImlhdCI6MTY3MDkyNDA4NSwidXNlcl9pZCI6ImZpbGlwIn0.XiDNEnkdljujjtRkElYmUIFkzf6ctxhEbDQB2DAPbGg",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "thierry",
            name = "Thierry",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci90aGllcnJ5IiwiaWF0IjoxNjcwOTI0MTAxLCJ1c2VyX2lkIjoidGhpZXJyeSJ9.4UgSogpG6vSqdaIxWu6p7N9pCweiwVkh-NzY3VL10yQ",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "martin",
            name = "Martin",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9tYXJ0aW4iLCJpYXQiOjE2NzA5MjQxMTQsInVzZXJfaWQiOiJtYXJ0aW4ifQ.fu7_35JefrLSraiXXS1oKSX8CX-mmxlkjEhhz45KU0k",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "oliver",
            name = "Oliver",
            imageUrl = "https://www.biografija.org/wp-content/uploads/2020/01/boban-rajovic.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9vbGl2ZXIiLCJpYXQiOjE2NzA5MjQyMDYsInVzZXJfaWQiOiJvbGl2ZXIifQ.QxMwqDoWEBO8jDmVc57pGdXE5w3YnKmHH2jV1DRIAxY",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "tomislav",
            name = "Tomislav",
            imageUrl = "https://i.scdn.co/image/ab67616d0000b2730ae491943a8668e81e212594",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci90b21pc2xhdiIsImlhdCI6MTY3MDkyNDQzOSwidXNlcl9pZCI6InRvbWlzbGF2In0.uMxSQkztftF0YcAk1967aUcrvD7Mc9KteDdrRQh3ADM",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        )
    )
}
