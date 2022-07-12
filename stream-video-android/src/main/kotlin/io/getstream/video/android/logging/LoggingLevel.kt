package io.getstream.video.android.logging

import okhttp3.logging.HttpLoggingInterceptor

/**
 * Represents and wraps the HTTP logging level for our API service.
 *
 * @property httpLoggingLevel The level of information logged by our HTTP interceptor.
 */
public enum class LoggingLevel(
    internal val httpLoggingLevel: HttpLoggingInterceptor.Level
) {
    NONE(HttpLoggingInterceptor.Level.NONE),
    BASIC(HttpLoggingInterceptor.Level.BASIC),
    BODY(HttpLoggingInterceptor.Level.BODY)
}