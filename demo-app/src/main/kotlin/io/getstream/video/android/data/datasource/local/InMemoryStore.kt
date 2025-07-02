package io.getstream.video.android.data.datasource.local

import io.getstream.video.android.model.User

interface InMemoryStore {

    fun saveUser(user: User)

    fun getUser(): User?
}