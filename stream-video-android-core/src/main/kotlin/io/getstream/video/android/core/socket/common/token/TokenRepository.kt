package io.getstream.video.android.core.socket.common.token

import io.getstream.video.android.core.socket.common.token.TokenManagerImpl.Companion.EMPTY_TOKEN

class TokenRepository(@Volatile private var token: String = EMPTY_TOKEN) {

    fun updateToken(token: String) {
        this.token = token
    }

    fun getToken(): String = token
}