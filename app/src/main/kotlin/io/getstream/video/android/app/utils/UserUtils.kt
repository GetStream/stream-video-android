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

import io.getstream.video.android.model.UserCredentials

fun getUsers(): List<UserCredentials> {
    return listOf(
        UserCredentials(
            id = "filip",
            name = "Filip",
            image = "https://avatars.githubusercontent.com/u/17215808?v=4",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9maWxpcCIsImlhdCI6MTY2MzgzNzcyNSwidXNlcl9pZCI6ImZpbGlwIn0.iv6C5qJKFyvWhbKsXKGVToyfo63P5fAyLJW0h517vSk",
            role = "admin"
        ),
        UserCredentials(
            id = "thierry",
            name = "Thierry",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci90aGllcnJ5IiwiaWF0IjoxNjYzODM3NzQxLCJ1c2VyX2lkIjoidGhpZXJyeSJ9.pPi5OaX8rOe2EoLa-mhUiV7ydudZnQZgggZv5b6E2dc",
            role = "admin"
        ),
        UserCredentials(
            id = "martin",
            name = "Martin",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9tYXJ0aW4iLCJpYXQiOjE2NjM4Mzc2NTEsInVzZXJfaWQiOiJtYXJ0aW4ifQ.Rk68sAq1y2onI1qcCiDGAUV8hZNUp9XCspwdalqu27Y",
            role = "admin"
        )
    )
}
