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
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9maWxpcCIsImlhdCI6MTY2NTY0NzMzMCwidXNlcl9pZCI6ImZpbGlwIn0.0JjbNmaL2F0oHulx9w6QLWb4HoxhTADa8n3SjHwvi9A",
            role = "admin"
        ),
        UserCredentials(
            id = "thierry",
            name = "Thierry",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci90aGllcnJ5IiwiaWF0IjoxNjY1NjQ3Mjk1LCJ1c2VyX2lkIjoidGhpZXJyeSJ9.HdWjv7d6Y3BxYr0p47nAiRnTA9AAsbF9VwV6hR1jvKw",
            role = "admin"
        ),
        UserCredentials(
            id = "martin",
            name = "Martin",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9tYXJ0aW4iLCJpYXQiOjE2NjU2NDczNTAsInVzZXJfaWQiOiJtYXJ0aW4ifQ.eG2_kPzmlcw1lC0_CM-FkBuFnp6U527nwJGXYmcyZ44",
            role = "admin"
        )
    )
}
