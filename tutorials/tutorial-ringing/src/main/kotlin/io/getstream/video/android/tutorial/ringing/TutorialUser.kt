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

package io.getstream.video.android.tutorial.ringing

import io.getstream.video.android.model.User

data class TutorialUser(
    val delegate: User,
    val token: String,
    val checked: Boolean = false,
) {
    val id: String get() = delegate.id
    val name: String get() = delegate.name
    val image: String get() = delegate.image

    companion object {
        val builtIn = listOf(
            TutorialUser(
                User(
                    id = "android-tutorial-1",
                    name = "User 1",
                    image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Willard%20Hessel.jpg",
                ),
                //token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0xIn0.3qB-FI6OAqf5ZEETtgs0XhaMmiaRF2jDJOqCVRsqqbc",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0xIn0.vDEb3CLKKA79Qfxvx28WldbihkmtXBJxgVjxQab2q5w",
            ),
            TutorialUser(
                User(
                    id = "android-tutorial-2",
                    name = "User 2",
                    image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Claudia%20Bradtke.jpg",
                ),
                //token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0yIn0.ksOq5ahC0745oZdPDKr2hyLp0j9exfwLE-AQITc9ZSc",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0yIn0.2XR4WtTyPtTPOKOrkaYbufJauzYjrZnp4kFGZ6bCPfA",
            ),
            TutorialUser(
                User(
                    id = "android-tutorial-3",
                    name = "User 3",
                    image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Bernard%20Windler.jpg",
                ),
                //token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0zIn0.g5h8coX8J1XUNHagPFoGBI0D7bN6P0w2Sd2rui89puE",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYW5kcm9pZC10dXRvcmlhbC0zIn0.K4oueQJv1Qmp2cQm9y1WslZfWAItJSA9BP7Fc0xmRlA",
            ),
        )
    }
}
