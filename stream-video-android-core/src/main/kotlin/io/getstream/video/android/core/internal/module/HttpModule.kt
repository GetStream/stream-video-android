/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.internal.module

import io.getstream.video.android.core.user.UserPreferences
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * @property loggingLevel Log level used for all HTTP requests towards the API.
 * @property preferences Used to fetch user based credentials.
 */
internal class HttpModule(
    private val loggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE,
    private val preferences: UserPreferences,
) {

    /**
     * Cached instance of the HTTP client.
     */
    internal val okHttpClient: OkHttpClient by lazy {
        buildOkHttpClient(preferences)
    }

    /**
     * New base url of the HTTP client.
     */
    internal var baseUrl: HttpUrl? = null

    /**
     * Builds the [OkHttpClient] used for all API calls.
     *
     * @param preferences The user-token and API key provider used to attach authorization
     * headers.
     * @return [OkHttpClient] that allows us API calls.
     */
    private fun buildOkHttpClient(preferences: UserPreferences): OkHttpClient {
        // TODO: Timeouts don't seem to be set here
        return OkHttpClient.Builder()
            .addInterceptor(
                buildCredentialsInterceptor(
                    preferences = preferences
                )
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel
                }
            )
            .addInterceptor(buildHostSelectionInterceptor())
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Builds the HTTP interceptor that adds headers to all API calls.
     *
     * @param preferences Provider of the user token and API key.
     *
     * @return [Interceptor] which adds headers.
     */
    private fun buildCredentialsInterceptor(
        preferences: UserPreferences
    ): Interceptor = Interceptor {
        val original = it.request()

        val token = if (original.url.toString().contains("sfu")) {
            preferences.getSfuToken()
        } else {
            preferences.getUserToken()
        }

        val updatedUrl = if (original.url.toString().contains("video")) {
            original.url.newBuilder()
                .addQueryParameter(API_KEY, preferences.getApiKey())
                .build()
        } else {
            original.url
        }

        val updated = original.newBuilder()
            .url(updatedUrl)
            .addHeader(HEADER_AUTHORIZATION, token)
            .header(STREAM_AUTH_TYPE, "jwt")
            .build()

        it.proceed(updated)
    }

    /**
     * Builds the HTTP interceptor that sets a new host from [baseUrl].
     *
     * @return [Interceptor] which replaces baseUrl.
     */
    private fun buildHostSelectionInterceptor(): Interceptor = Interceptor { chain ->
        val baseUrl = baseUrl ?: return@Interceptor chain.proceed(chain.request())
        val original = chain.request()
        if (original.url.host == REPLACEMENT_HOST) {
            val updatedBaseUrl = original.url.newBuilder()
                .host(baseUrl.host)
                .build()
            val updated = original.newBuilder()
                .url(updatedBaseUrl)
                .build()
            chain.proceed(updated)
        } else {
            chain.proceed(chain.request())
        }
    }

    companion object {

        /**
         * Host pattern to be replaced.
         */
        private const val REPLACEMENT_HOST = "replacement.url"

        /**
         * Url pattern to be replaced.
         */
        internal const val REPLACEMENT_URL = "https://$REPLACEMENT_HOST"

        /**
         * Key used to prove authorization to the API.
         */
        private const val HEADER_AUTHORIZATION = "Authorization"

        /**
         * Query key used to authenticate to the API.
         */
        private const val API_KEY = "api_key"
        private const val STREAM_AUTH_TYPE = "stream-auth-type"

        /**
         * Instance of the module, that's reused for HTTP communication.
         */
        private lateinit var module: HttpModule

        internal fun create(
            loggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE,
            credentialsProvider: UserPreferences
        ): HttpModule {
            return HttpModule(loggingLevel, credentialsProvider)
        }

        /**
         * Returns an instance of the module for HTTP communication. If one doesn't exists, creates
         * the instance and then returns it.
         */
        internal fun getOrCreate(
            loggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE,
            credentialsProvider: UserPreferences,
            forceCreate: Boolean = false
        ): HttpModule {
            if (this::module.isInitialized.not() || forceCreate) {
                module = create(loggingLevel, credentialsProvider)
            }

            return module
        }
    }
}
