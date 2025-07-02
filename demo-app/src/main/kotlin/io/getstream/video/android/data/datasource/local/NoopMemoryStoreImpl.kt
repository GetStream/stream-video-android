package io.getstream.video.android.data.datasource.local

import io.getstream.video.android.model.User

class NoopMemoryStoreImpl : InMemoryStore {

    override fun saveUser(user: User) {}

    override fun getUser(): User? = null
}