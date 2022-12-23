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
            id = "djuropalica",
            name = "Djuro Palica",
            imageUrl = "https://www.clipartmax.com/png/middle/354-3544458_post-profile-pic-for-meme-page.png",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9kanVyb3BhbGljYSIsImlhdCI6MTY3MTE5MzQ5OCwidXNlcl9pZCI6ImRqdXJvcGFsaWNhIn0.Sc0FvkGrCJMHR9hxh72w0j7J4AwtMQ8eEuwFTgOsc8I",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "georgegeorgorovic",
            name = "George Georgorovic",
            imageUrl = "https://resizing.flixster.com/WPmZJiBi-xmd9ZRsuSdM8tU064k=/206x305/v2/https://flxt.tmsimg.com/assets/p16599906_e_v10_aa.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9nZW9yZ2VnZW9yZ29yb3ZpYyIsImlhdCI6MTY3MTE5MzUzNiwidXNlcl9pZCI6Imdlb3JnZWdlb3Jnb3JvdmljIn0.TSUFs9pMHKOyK23W3yMea1KaSSTVDGMrU2PUUiMDDVw",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "willpantera",
            name = "Will Pantera",
            imageUrl = "https://w0.peakpx.com/wallpaper/299/280/HD-wallpaper-this-is-fine-dreams-wolf-thumbnail.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci93aWxscGFudGVyYSIsImlhdCI6MTY3MTE5MzYwMiwidXNlcl9pZCI6IndpbGxwYW50ZXJhIn0.ysynfYTvhwdvYVhjo1LoNM5BJhumx_pMis8lIcjtS5o",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        )
    )
}
