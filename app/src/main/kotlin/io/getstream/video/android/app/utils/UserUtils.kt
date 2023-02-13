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
            id = "djuropalica",
            name = "Djuro Palica",
            imageUrl = "https://ichef.bbci.co.uk/news/976/cpsprodpb/16620/production/_91408619_55df76d5-2245-41c1-8031-07a4da3f313f.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9kanVyb3BhbGljYSIsImlhdCI6MTY3NTA3NjQyOSwidXNlcl9pZCI6ImRqdXJvcGFsaWNhIn0.gHeBCWb2QIQejmgh1CAFKIx39EqjephifrNUnJEi4ew",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "georgegeorgorovic",
            name = "George Georgorovic",
            imageUrl = "https://resizing.flixster.com/WPmZJiBi-xmd9ZRsuSdM8tU064k=/206x305/v2/https://flxt.tmsimg.com/assets/p16599906_e_v10_aa.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9nZW9yZ2VnZW9yZ29yb3ZpYyIsImlhdCI6MTY3NTA3NjQzNiwidXNlcl9pZCI6Imdlb3JnZWdlb3Jnb3JvdmljIn0.r1DOfWOhl1PIZt3FXgaZbhCtGdk9xMBP7H2nMoYNvWI",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        ),
        User(
            id = "willpantera",
            name = "Will Pantera",
            imageUrl = "https://w0.peakpx.com/wallpaper/299/280/HD-wallpaper-this-is-fine-dreams-wolf-thumbnail.jpg",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci93aWxscGFudGVyYSIsImlhdCI6MTY3NTA3NjQ0MiwidXNlcl9pZCI6IndpbGxwYW50ZXJhIn0.mvu7V8MbOD_7s4OY2rdvgcv47x0BjERFfg6RYa5pDWI",
            role = "admin",
            teams = emptyList(),
            extraData = emptyMap()
        )
    )
}
