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
            id = "tomislav",
            name = "Tomislav",
            imageUrl = "",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci90b21pc2xhdiIsImlhdCI6MTY2OTI4Mjg4MywidXNlcl9pZCI6InRvbWlzbGF2In0.-_Vik2BG8aA28b7CtKrU901V-g1daIL9fQ6Kq-4sMHI",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "tomislav2",
            name = "Tomislav Clone",
            imageUrl = "",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci90b21pc2xhdjIiLCJpYXQiOjE2NjkyODI4ODcsInVzZXJfaWQiOiJ0b21pc2xhdjIifQ.U_ydsiXscIG3WkX6O33L0Um0fteu3AuTGrJFRvoNT-4",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "tomislav3",
            name = "Tomislav Clone Clone",
            imageUrl = "",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci90b21pc2xhdjMiLCJpYXQiOjE2NjkyODI4OTAsInVzZXJfaWQiOiJ0b21pc2xhdjMifQ.u6mhWyX_uMOal9vM2DTaDXFpAjIoeAAj9N_TEuygakk",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        )
    )
}
