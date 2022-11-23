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
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci92YXNpbCIsImlhdCI6MTY2OTIwMDMyNiwidXNlcl9pZCI6InZhc2lsIn0.ko8cIAZQyy1RZXt6fN6pYkqG6XJxmeNfT9qzHcABF-Q",
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
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci92ZXNlbGluIiwiaWF0IjoxNjY5MjAwMzUwLCJ1c2VyX2lkIjoidmVzZWxpbiJ9.ytKhF1t66ur8lqQKizd3x6-pKTs9KIAYHRjJbpZ1Sig",
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
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci92YWxpYSIsImlhdCI6MTY2OTIwMDM3MiwidXNlcl9pZCI6InZhbGlhIn0.9INmMZxU2QDn5_ryJPAwPPvJCnMkTdkJL63y_tJPoUc",
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
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9kYW1qYW4iLCJpYXQiOjE2NjkyMDAzOTMsInVzZXJfaWQiOiJkYW1qYW4ifQ.O0B6ffvEr-9FZqwhIiUr_IIYIWhxAVvQYTsuWa_VaTE",
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
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9qb3JkYW4iLCJpYXQiOjE2NjkyMDA0MDgsInVzZXJfaWQiOiJqb3JkYW4ifQ.CRgu0wBtluXo7awRNzkim5zEklLKou2UXXv3BIelcVw",
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
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdHJlYW0tdmlkZW8tZ29AdjAuMS4wIiwic3ViIjoidXNlci9pbmEiLCJpYXQiOjE2NjkyMDA0MjAsInVzZXJfaWQiOiJpbmEifQ.lXeBykboLPuqIH8WWQh-xp4jlkwxQ4V9IGpzbLh0R5M",
                extraData = mapOf(
                    "chatToken" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiaW5hIn0.3mTkk94zpzGbSHdkRXb_UHqboTq06WZ5zqDH8xtgyyg"
                ),
                teams = emptyList()
            ),
        )
    }

    override val userState: StateFlow<List<User>> = MutableStateFlow(provideUsers())
}