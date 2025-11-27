/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.data.services.stream

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.getstream.log.streamLog
import io.getstream.video.android.model.User
import io.getstream.video.android.models.UserCredentials
import io.getstream.video.android.models.builtInCredentials
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

fun interface StreamService {
    @GET("api/auth/create-token")
    suspend fun getAuthData(
        @Query("environment") environment: String,
        @Query("user_id") userId: String?,
        @Query("exp") exp: Int,
    ): GetAuthDataResponse

    companion object {
        private const val BASE_URL = "https://pronto.getstream.io/"
        public const val TOKEN_EXPIRY_TIME = 20

        private val json = Json { ignoreUnknownKeys = true }
        private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor {
                streamLog(tag = "Video:Http") { it }
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        private val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(okHttpClient)
            .build()

        private val serviceInstance = retrofit.create<StreamService>()

        val instance = StreamService { environment, userId, exp ->
            User.builtInCredentials[userId]?.toAuthDataResponse()
                ?: serviceInstance.getAuthData(environment, userId, exp)
        }
    }
}

private fun UserCredentials.toAuthDataResponse(): GetAuthDataResponse {
    return GetAuthDataResponse(userId, apiKey, token)
}
