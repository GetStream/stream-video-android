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
