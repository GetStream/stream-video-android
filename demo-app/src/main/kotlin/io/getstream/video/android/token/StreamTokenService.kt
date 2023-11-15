/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.token

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

interface StreamTokenService {
    @GET("api/auth/create-token")
    suspend fun fetchToken(
        @Query("environment") environment: String,
        @Query("user_id") userId: String?
    ): TokenResponse
}

object StreamVideoNetwork {
    private val contentType = "application/json".toMediaType()
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val retrofit =
        Retrofit.Builder()
            .baseUrl("https://pronto.getstream.io/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

    val tokenService = retrofit.create<StreamTokenService>()
}
