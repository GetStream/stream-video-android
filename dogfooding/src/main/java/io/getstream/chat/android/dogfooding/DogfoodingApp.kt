package io.getstream.chat.android.dogfooding

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.StreamCallsBuilder
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider

class DogfoodingApp : Application() {

    private var credentials: CredentialsProvider? = null
    private var calls: StreamCalls? = null

    val credentialsProvider: CredentialsProvider
        get() = requireNotNull(credentials)

    val streamCalls: StreamCalls
        get() = requireNotNull(calls)

    /**
     * Sets up and returns the [streamCalls] required to connect to the API.
     */
    fun initializeStreamCalls(
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamCalls {
        this.credentials = credentialsProvider

        return StreamCallsBuilder(
            context = this,
            credentialsProvider = credentialsProvider,
            loggingLevel = loggingLevel
        ).build().also {
            calls = it
        }
    }

    fun logOut() {
        FirebaseAuth.getInstance().signOut()
        credentialsProvider
    }
}

val Context.dogfoodingApp get() = applicationContext as DogfoodingApp