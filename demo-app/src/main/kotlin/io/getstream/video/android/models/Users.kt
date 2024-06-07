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

public fun User.Companion.builtInUsers(): List<User> {
    return listOf<User>(
        User(
            id = "alex",
            name = "Alex",
            role = "user",
            image = "https://ca.slack-edge.com/T02RM6X6B-U05UD37MA1G-f062f8b7afc2-512",
        ),
        User(
            id = "kanat",
            name = "Kanat",
            role = "user",
            image = "https://ca.slack-edge.com/T02RM6X6B-U034NG4FPNG-9a37493e25e0-512",
        ),
        User(
            id = "valia",
            name = "Bernard Windler",
            role = "user",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Bernard%20Windler.jpg",
        ),
        User(
            id = "vasil",
            name = "Willard Hesser",
            role = "user",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Willard%20Hessel.jpg",
        ),
    )
}
