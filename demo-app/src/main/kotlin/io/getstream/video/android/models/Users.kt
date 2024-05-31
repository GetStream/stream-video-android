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