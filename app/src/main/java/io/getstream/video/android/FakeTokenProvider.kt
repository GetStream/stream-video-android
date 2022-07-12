package io.getstream.video.android

import io.getstream.video.android.token.TokenProvider

class FakeTokenProvider: TokenProvider {

    override fun provideUserToken(): String {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidG9tbWFzbyJ9.XGkxJKi33fHr3cHyLFc6HRnbPgLuwNHuETWQ2MWzz5c"
    }
}