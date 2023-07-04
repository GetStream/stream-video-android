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

package io.getstream.video.android.app

import android.app.Application
import android.content.Context
import io.getstream.video.android.app.network.StreamVideoNetwork
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User

class VideoApp : Application() {

    /** Sets up and returns the [StreamVideo] required to connect to the API. */
    fun initializeStreamVideo(
        user: User,
        apiKey: ApiKey,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        return StreamVideoBuilder(
            context = this,
            user = user,
            apiKey = apiKey,
            loggingLevel = loggingLevel,
            ensureSingleInstance = false,
            tokenProvider = {
                val email = user.custom["email"]
                val response = StreamVideoNetwork.tokenService.fetchToken(
                    userId = email,
                    apiKey = API_KEY
                )
                response.token
            }
        ).build()
    }

    fun logOut() {
        StreamVideo.instance().logOut()
    }

    companion object {
        const val API_KEY = BuildConfig.SAMPLE_STREAM_VIDEO_API_KEY
    }
}

internal val Context.videoApp get() = applicationContext as VideoApp
