package io.getstream.chat.android.dogfooding

import android.content.SharedPreferences
import io.getstream.video.android.model.UserCredentials

class UserPreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : UserPreferences {

    override fun getCachedCredentials(): UserCredentials {
        return UserCredentials(
            id = sharedPreferences.getString(KEY_ID, "") ?: "",
            token = sharedPreferences.getString(KEY_TOKEN, "") ?: "",
            image = sharedPreferences.getString(KEY_IMAGE, "") ?: "",
            name = sharedPreferences.getString(KEY_NAME, "") ?: "",
            role = sharedPreferences.getString(KEY_ROLE, "") ?: ""
        )
    }

    override fun storeUserCredentials(userCredentials: UserCredentials) {
        val editor = sharedPreferences.edit()

        editor.putString(KEY_ID, userCredentials.id)
        editor.putString(KEY_TOKEN, userCredentials.token)
        editor.putString(KEY_IMAGE, userCredentials.image)
        editor.putString(KEY_NAME, userCredentials.name)
        editor.putString(KEY_ROLE, userCredentials.role)

        editor.apply()
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    internal companion object {
        private const val KEY_ID = "id"
        private const val KEY_TOKEN = "token"
        private const val KEY_IMAGE = "image"
        private const val KEY_NAME = "name"
        private const val KEY_ROLE = "role"
    }
}