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

package io.getstream.video.chat_with_video_sample.users

import io.getstream.video.android.model.User
import io.getstream.video.android.user.UsersProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUsersProvider : UsersProvider {

    override fun provideUsers(): List<User> {
        return mockUsers()
    }

    private fun mockUsers(): List<User> {
        return listOf(
            User(
                id = "vasil",
                name = "Vasil Valkanov",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/VASIL_VALKANOV.jpg",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci92YXNpbCIsImlhdCI6MTY3MDkyOTk4MSwidXNlcl9pZCI6InZhc2lsIn0.YlbxW8zCgTPDUe_rofCta2eKubF-Km5xqsJi_geDsv0",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidmFzaWwifQ.7xb2ns3CWqX1XpYwJy89OHyARuIvouISpEoUTRwvZGg"
                ),
                teams = emptyList()
            ),
            User(
                id = "veselin",
                name = "Veselin Marinov",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/Veselin_Marinov_2021-08-04.jpg",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci92ZXNlbGluIiwiaWF0IjoxNjcwOTI5OTk4LCJ1c2VyX2lkIjoidmVzZWxpbiJ9.-scF4--7n3NGRE3wj5vfj7bJ4n5aWcYKdVg5uSqVNGc",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidmVzZWxpbiJ9.8WJIIBHinLz80cSi_qy-xj45rT60nGdCBmarW7KEGeU"
                ),
                teams = emptyList()
            ),
            User(
                id = "valia",
                name = "Valia",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/Valia_-_Site_April.jpg",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci92YWxpYSIsImlhdCI6MTY3MDkzMDAxNywidXNlcl9pZCI6InZhbGlhIn0.SMz_Dggqf8ppxov3rjZtcs88v4FmKits0shVTfyObUQ",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidmFsaWEifQ.MCm5GWwNeqOWdXnAReXt_9v7nIH7Xg6c94uBg1dxMOk"
                ),
                teams = emptyList()
            ),
            User(
                id = "damjan",
                name = "Damjan Popov",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/DAMYAN_POPOV.jpg",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9kYW1qYW4iLCJpYXQiOjE2NzA5MzAwMzMsInVzZXJfaWQiOiJkYW1qYW4ifQ.ECGyzMLH1ZQEaWiM7iyGsdh3bdubcJRXwGBLZlYJlXM",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiZGFtamFuIn0.BSeX6DPXC3YfVjHAf8gzl2hJ532DFmrJEhqT3pFLY3c"
                ),
                teams = emptyList()
            ),
            User(
                id = "jordan",
                name = "Jordan",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/DJORDAN_-_SEPT_-_2022.jpg",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9qb3JkYW4iLCJpYXQiOjE2NzA5MzAwNTIsInVzZXJfaWQiOiJqb3JkYW4ifQ.reRIu94p1nJnR-487zC3ySEFeV1Er7hK3I-1b13mIyA",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiam9yZGFuIn0.wwqn1Y2rwcDlO4-U3pmurIpK6CrIT0TQFvI4XovER88"
                ),
                teams = emptyList()
            ),
            User(
                id = "ina",
                name = "Ina Garjadi",
                role = "admin",
                imageUrl = "https://payner.bg/images/uploads/Artist_images/INA_GAYARDI_-_PAYNER_-_OCTOBER_-_2022.jpg",
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tZ28tY2xpZW50LTAuMC4xIiwic3ViIjoidXNlci9pbmEiLCJpYXQiOjE2NzA5MzAwNjQsInVzZXJfaWQiOiJpbmEifQ.FTVAMAWsKoO413lPIIzgbHsxrM-q-I5OO_bEYoeDfYo",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiaW5hIn0.3mTkk94zpzGbSHdkRXb_UHqboTq06WZ5zqDH8xtgyyg"
                ),
                teams = emptyList()
            ),
        )
    }

    override val userState: StateFlow<List<User>> = MutableStateFlow(provideUsers())
}
