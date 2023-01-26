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

package io.getstream.video.video_with_chat_final.application

import android.app.Application
import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.attachments.StreamAttachmentFactories
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.user.UserCredentialsManager
import io.getstream.video.android.user.UsersProvider
import io.getstream.video.video_with_chat_final.users.FakeUsersProvider

class VideoWithChatApp : Application() {

    val usersLoginProvider: UsersProvider by lazy { FakeUsersProvider() }

    val chatClient: ChatClient by lazy {
        TODO("Implement ChatClient")
    }

    val attachmentFactories by lazy {
        StreamAttachmentFactories.defaultFactories() // TODO add custom attachment
    }

    lateinit var credentialsProvider: CredentialsProvider
        private set

    lateinit var streamVideo: StreamVideo
        private set

    fun initializeStreamVideo(
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        TODO()
    }

    fun logOut() {
        val preferences = UserCredentialsManager.initialize(this)

        // TODO log out of clients
        preferences.clear()
    }
}

internal const val API_KEY = "us83cfwuhy8n"

internal val Context.videoWithChatApp get() = applicationContext as VideoWithChatApp
