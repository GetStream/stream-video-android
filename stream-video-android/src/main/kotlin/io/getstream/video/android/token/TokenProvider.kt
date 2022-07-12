package io.getstream.video.android.token

/**
 * Exposes a way to build a token provided that connects to custom implementation for
 * authentication.
 */
public interface TokenProvider {

    /**
     * @return The user token backed by authentication services.
     */
    public fun provideUserToken(): String
}