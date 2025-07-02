package io.getstream.video.android.data.datasource.local

import io.getstream.video.android.model.User

/**
 * Only used in E2E Testing builds
 */
class InMemoryStoreImpl : InMemoryStore {
    private var temporaryUser: User? = null

    override fun saveUser(user: User) {
        temporaryUser = user
    }

    override fun getUser(): User? {
        return temporaryUser
    }
}