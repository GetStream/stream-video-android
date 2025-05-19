/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.models

import io.getstream.video.android.model.User

data class UserCredentials(val userId: String, val apiKey: String, val token: String)

public val User.Companion.builtInCredentials: Map<String, UserCredentials>
    get() = mapOf()

public fun User.Companion.builtInUsers(): List<User> {
    return listOf(
        User(
            id = "thierry",
            name = "Thierry",
            role = "user",
            image = "https://getstream.io/static/237f45f28690696ad8fff92726f45106/c59de/thierry.webp",
        ),
        User(
            id = "tommaso",
            name = "Tommaso",
            role = "user",
            image = "https://getstream.io/static/712bb5c0bd5ed8d3fa6e5842f6cfbeed/c59de/tommaso.webp",
        ),
        User(
            id = "martin",
            name = "Martin",
            role = "user",
            image = "https://getstream.io/static/2796a305dd07651fcceb4721a94f4505/802d2/martin-mitrevski.webp",
        ),
        User(
            id = "ilias",
            name = "Ilias",
            role = "user",
            image = "https://getstream.io/static/62cdddcc7759dc8c3ba5b1f67153658c/802d2/ilias-pavlidakis.webp",
        ),
        User(
            id = "marcelo",
            name = "Marcelo",
            role = "user",
            image = "https://getstream.io/static/aaf5fb17dcfd0a3dd885f62bd21b325a/802d2/marcelo-pires.webp",
        ),
        User(
            id = "alex",
            name = "Alex",
            role = "user",
            image = "https://ca.slack-edge.com/T02RM6X6B-U05UD37MA1G-f062f8b7afc2-512",
        ),
        User(
            id = "liviu",
            name = "Liviu",
            role = "user",
            image = "https://ca.slack-edge.com/T02RM6X6B-U0604NCKKRA-76f99b6ba2c8-512",
        ),
        User(
            id = "kanat",
            name = "Kanat",
            role = "user",
            image = "https://ca.slack-edge.com/T02RM6X6B-U034NG4FPNG-9a37493e25e0-512",
        ),
    )
}
