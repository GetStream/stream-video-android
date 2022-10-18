package io.getstream.chat.android.dogfooding

import io.getstream.video.android.model.UserCredentials

interface UserPreferences {

    fun getCachedCredentials(): UserCredentials

    fun storeUserCredentials(userCredentials: UserCredentials)

    fun clear()
}
