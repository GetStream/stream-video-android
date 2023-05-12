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

package io.getstream.video.chat_with_video_starter.application

import android.app.Application
import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.compose.ui.attachments.StreamAttachmentFactories
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.user.UsersProvider
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.chat_with_video_starter.BuildConfig
import io.getstream.video.chat_with_video_starter.users.FakeUsersProvider

class ChatWithVideoApp : Application() {

    val usersLoginProvider: UsersProvider by lazy { FakeUsersProvider() }

    val chatClient: ChatClient by lazy { TODO("Implement ChatClient") }

    val attachmentFactories by lazy {
        StreamAttachmentFactories.defaultFactories() // TODO add custom attachment
    }

    private var video: StreamVideo? = null

    val streamVideo: StreamVideo
        get() = requireNotNull(video)

    fun initializeStreamVideo(user: User, apiKey: ApiKey, loggingLevel: LoggingLevel): StreamVideo {
        TODO()
    }

    fun logOut() {
        // TODO log out of clients
    }
}

internal const val API_KEY = BuildConfig.SAMPLE_STREAM_VIDEO_API_KEY

internal val Context.chatWithVideoApp
    get() = applicationContext as ChatWithVideoApp
