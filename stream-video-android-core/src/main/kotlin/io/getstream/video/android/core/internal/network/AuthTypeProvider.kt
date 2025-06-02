package io.getstream.video.android.core.internal.network

internal class AuthTypeProvider {
    private var authType: AuthType = AuthType.JWT

    fun getAuthType(): String {
        return authType.value
    }

    fun setAuthType(authType: AuthType) {
        this.authType = authType
    }

    internal enum class AuthType(val value: String) {
        JWT("jwt"),
        ANONYMOUS("anonymous"),
    }
}