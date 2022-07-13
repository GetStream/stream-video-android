/*
 * Copyright 2022 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android

import android.app.Application
import io.getstream.video.android.client.VideoClient
import io.getstream.video.android.logging.LoggingLevel
import stream.video.User

class VideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        videoClient = VideoClient
            .Builder(
                apiKey = "fake-api-key",
                appContext = instance,
                user = buildFakeUser(),
                tokenProvider = FakeTokenProvider()
            )
            .loggingLevel(LoggingLevel.BODY)
            .build()
    }

    private fun buildFakeUser(): User {
        return User(id = "tommaso")
    }

    companion object {
        lateinit var instance: VideoApp

        lateinit var videoClient: VideoClient
            private set
    }
}
