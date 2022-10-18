package io.getstream.chat.android.dogfooding

import io.getstream.video.android.model.User
import io.getstream.video.android.token.CredentialsProvider

class AuthCredentialsProvider(
    private val apiKey: String,
    private val userToken: String,
    private val user: User
) : CredentialsProvider {

    private var sfuToken: String? = null

    override fun loadToken(): String {
        return userToken
    }

    override fun getCachedToken(): String {
        return userToken
    }

    override fun loadApiKey(): String {
        return apiKey
    }

    override fun getCachedApiKey(): String {
        return apiKey
    }

    override fun setSfuToken(token: String?) {
        this.sfuToken = token
    }

    override fun getSfuToken(): String {
        return sfuToken ?: ""
    }

    override fun getUserCredentials(): User {
        return user
    }
}