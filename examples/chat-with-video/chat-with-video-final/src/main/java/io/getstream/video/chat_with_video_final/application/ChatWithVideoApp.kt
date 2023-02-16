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

package io.getstream.video.chat_with_video_final.application

import android.app.Application
import android.content.Context
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.compose.ui.attachments.StreamAttachmentFactories
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.log.StreamLog
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.StreamVideoConfig
import io.getstream.video.android.core.input.CallActivityInput
import io.getstream.video.android.core.input.CallServiceInput
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.user.UsersProvider
import io.getstream.video.chat_with_video_final.ui.call.CallActivity
import io.getstream.video.chat_with_video_final.ui.call.CallService
import io.getstream.video.chat_with_video_final.ui.messages.attachment.CallAttachmentFactory
import io.getstream.video.chat_with_video_final.users.FakeUsersProvider

class ChatWithVideoApp : Application() {

    private val videoConfig: StreamVideoConfig = object : StreamVideoConfig {
        override val dropTimeout: Long = 30_000L
        override val cancelOnTimeout: Boolean = true
        override val joinOnAcceptedByCallee: Boolean = true
        override val createCallClientInternally: Boolean = true

        override val autoPublish: Boolean = true
        override val defaultAudioOn: Boolean = false
        override val defaultVideoOn: Boolean = true
        override val defaultSpeakerPhoneOn: Boolean = false
    }

    val usersLoginProvider: UsersProvider by lazy { FakeUsersProvider() }

    val chatClient: ChatClient by lazy {
        val offlinePlugin = StreamOfflinePluginFactory(this)

        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = this
        )

        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING

        ChatClient.Builder("tp8sef43xcpc", this)
            .withPlugins(offlinePlugin, statePluginFactory)
            .logLevel(logLevel)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .build()
    }

    val attachmentFactories by lazy {
        listOf(CallAttachmentFactory()) + StreamAttachmentFactories.defaultFactories()
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StreamLog.setValidator { _, _ -> true }
            StreamLog.install(AndroidStreamLogger())
        }
    }

    private var video: StreamVideo? = null

    val streamVideo: StreamVideo
        get() = requireNotNull(video)

    fun initializeStreamVideo(
        user: io.getstream.video.android.core.model.User,
        apiKey: ApiKey,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        return StreamVideoBuilder(
            context = this,
            user = user,
            apiKey = apiKey,
            androidInputs = setOf(
                CallServiceInput.from(CallService::class),
                CallActivityInput.from(CallActivity::class),
            ),
            loggingLevel = loggingLevel,
            config = videoConfig
        ).build().also {
            video = it
        }
    }

    fun logOut() {
        chatClient.disconnect(true).enqueue()
        streamVideo.logOut()
        video = null
    }
}

internal const val API_KEY = "us83cfwuhy8n"

internal val Context.chatWithVideoApp get() = applicationContext as ChatWithVideoApp
