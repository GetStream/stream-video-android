package io.getstream.chat.android.dogfooding

import android.app.Application
import android.content.Context
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.StreamCallsBuilder
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider

class DogfoodingApp : Application() {

    private lateinit var credentialsProvider: CredentialsProvider
    private lateinit var streamCalls: StreamCalls

    /**
     * Sets up and returns the [streamCalls] required to connect to the API.
     */
    fun initializeStreamCalls(
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamCalls {
        this.credentialsProvider = credentialsProvider

        return StreamCallsBuilder(
            context = this,
            credentialsProvider = credentialsProvider,
            loggingLevel = loggingLevel
        ).build().also {
            streamCalls = it
        }
    }
}

public val Context.dogfoodingApp get() = applicationContext as DogfoodingApp