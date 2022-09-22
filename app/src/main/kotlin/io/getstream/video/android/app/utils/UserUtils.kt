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
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9maWxpcCIsImV4cCI6MTY2MzgzNjczOSwiaWF0IjoxNjYzNzUwMzM5LCJ1c2VyX2lkIjoiZmlsaXAifQ.RWUl_J0nLnEa7dDiiJBFJ76CyC6sraHfyOyrknPU0nA"
        ),
        UserCredentials(
            id = "thierry",
            name = "Thierry",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Mile_Kitic_from_BISO0675.jpg/300px-Mile_Kitic_from_BISO0675.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci90aGllcnJ5IiwiZXhwIjoxNjYzODM2OTQwLCJpYXQiOjE2NjM3NTA1NDAsInVzZXJfaWQiOiJ0aGllcnJ5In0.Dld5KbGYv0UuotGRp426GNv97SziSydzyE2DmGj3c_U"
        ),
        UserCredentials(
            id = "martin",
            name = "Martin",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9tYXJ0aW4iLCJleHAiOjE2NjM4MzY5NTcsImlhdCI6MTY2Mzc1MDU1NywidXNlcl9pZCI6Im1hcnRpbiJ9.uFPhRUHYzzkhTRxZ424hrnyHJj9HS_2Jcg7QIqsyzyE"
        )
    )
}
