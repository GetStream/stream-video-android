package io.getstream.video.android.dogfooding

import android.app.Application
import android.content.Context
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.StreamCallsBuilder
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider

class VideoApp : Application() {

    // 1
    private var credentials: CredentialsProvider? = null
    private var calls: StreamCalls? = null

    val credentialsProvider: CredentialsProvider
        get() = requireNotNull(credentials)

    val streamCalls: StreamCalls
        get() = requireNotNull(calls)

    /**
     * Sets up and returns the [streamCalls] required to connect to the API.
     */
    fun initializeStreamCalls( // 2
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamCalls {
        this.credentials = credentialsProvider

        return StreamCallsBuilder(
            context = this,
            credentialsProvider = credentialsProvider,
            loggingLevel = loggingLevel,
        ).build().also {
            calls = it
        }
    }
}

// 4
val Context.videoApp get() = applicationContext as VideoApp